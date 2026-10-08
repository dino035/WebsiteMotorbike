package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.Notification;
import com.example.websitemotorbike.model.NotificationType;
import com.example.websitemotorbike.model.OnlinePayment;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.model.OrderStatus;
import com.example.websitemotorbike.model.Payment;
import com.example.websitemotorbike.model.PaymentMethod;
import com.example.websitemotorbike.model.PaymentStatus;
import com.example.websitemotorbike.util.DBUtil;
import com.example.websitemotorbike.util.VNPayUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thanh toán trực tuyến qua VNPay. Mọi thay đổi đều khóa dòng đơn hàng trước (PESSIMISTIC_WRITE), giống OrderDB,
 * để việc trả tiền, hủy đơn và đổi sang COD không giẫm lên nhau.
 */
public class OnlinePaymentDB {

    /** Kết quả xử lý một thông báo kết quả từ VNPay. */
    public enum Outcome {
        /** Không có giao dịch nào mang mã này. */
        NOT_FOUND,
        /** Số tiền VNPay báo khác số tiền của giao dịch: không ghi nhận gì. */
        AMOUNT_MISMATCH,
        /** Giao dịch đã được xử lý trước đó (trình duyệt quay lại và IPN đến hai lần). */
        ALREADY_PROCESSED,
        /** Trả tiền thành công, đơn hàng chuyển sang đã thanh toán. */
        PAID,
        /** Trả tiền thành công nhưng đơn đã bị hủy trước đó: cần hoàn tiền thủ công. */
        PAID_ORDER_CANCELLED,
        /** Trả tiền thành công nhưng đơn đã được thanh toán bằng lần khác: cần hoàn tiền thủ công. */
        PAID_DUPLICATE,
        /** Khách hủy, hết hạn hoặc ngân hàng từ chối. */
        FAILED
    }

    public static class Result {
        private final Outcome outcome;
        private final OnlinePayment attempt;
        private final Order order;

        Result(Outcome outcome, OnlinePayment attempt, Order order) {
            this.outcome = outcome;
            this.attempt = attempt;
            this.order = order;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public OnlinePayment getAttempt() {
            return attempt;
        }

        public Order getOrder() {
            return order;
        }
    }

    /**
     * Bắt đầu một lần trả tiền VNPay cho đơn của accountId: tạo bản ghi PENDING với mã giao dịch mới.
     * Được gọi khi đặt hàng chọn VNPay và khi khách bấm "Thanh toán lại".
     *
     * @throws IllegalArgumentException đơn không tồn tại, đã hủy, không dùng VNPay hoặc đã thanh toán
     * @throws IllegalStateException    lỗi CSDL
     */
    public static OnlinePayment startAttempt(String accountId, String orderId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            Order order = em.find(Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !order.getAccountId().equals(accountId)) {
                throw new IllegalArgumentException("Không tìm thấy đơn hàng");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Đơn hàng đã hủy, không thể thanh toán");
            }
            Payment payment = findPayment(em, orderId);
            if (payment == null || payment.getMethod() != PaymentMethod.VNPAY) {
                throw new IllegalArgumentException("Đơn hàng này không thanh toán bằng VNPay");
            }
            if (payment.getStatus() == PaymentStatus.PAID) {
                throw new IllegalArgumentException("Đơn hàng này đã được thanh toán");
            }
            if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
                throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ");
            }

            OnlinePayment attempt = new OnlinePayment();
            attempt.setOrderId(orderId);
            attempt.setAccountId(accountId);
            attempt.setTxnRef(UUID.randomUUID().toString().replace("-", ""));
            attempt.setAmount(payment.getAmount());
            attempt.setStatus(PaymentStatus.PENDING);
            attempt.setCreatedAt(LocalDateTime.now());
            em.persist(attempt);
            trans.commit();
            return attempt;
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể bắt đầu thanh toán, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /**
     * Ghi nhận kết quả VNPay. Phải gọi SAU KHI đã kiểm tra chữ ký và vnp_TmnCode.
     * Chạy lặp lại (trình duyệt quay lại, IPN, tải lại trang) không làm gì thêm.
     */
    public static Result complete(Map<String, String> params) {
        OnlinePayment found = selectByTxnRef(params.get("vnp_TxnRef"));
        if (found == null) {
            return new Result(Outcome.NOT_FOUND, null, null);
        }

        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        Order order;
        OnlinePayment attempt;
        Outcome outcome;
        try {
            order = em.find(Order.class, found.getOrderId(), LockModeType.PESSIMISTIC_WRITE);
            attempt = em.find(OnlinePayment.class, found.getAttemptId(), LockModeType.PESSIMISTIC_WRITE);
            if (order == null || attempt == null) {
                throw new IllegalArgumentException("Không tìm thấy giao dịch");
            }
            if (attempt.getStatus() != PaymentStatus.PENDING) {
                rollback(trans);
                return new Result(Outcome.ALREADY_PROCESSED, attempt, order);
            }
            if (!String.valueOf(VNPayUtil.toVnpAmount(attempt)).equals(params.get("vnp_Amount"))) {
                rollback(trans);
                return new Result(Outcome.AMOUNT_MISMATCH, attempt, order);
            }

            String transactionStatus = params.get("vnp_TransactionStatus");
            boolean success = "00".equals(params.get("vnp_ResponseCode"))
                    && (transactionStatus == null || transactionStatus.isEmpty() || "00".equals(transactionStatus));

            LocalDateTime now = LocalDateTime.now();
            attempt.setResponseCode(params.get("vnp_ResponseCode"));
            attempt.setTransactionNo(params.get("vnp_TransactionNo"));
            attempt.setBankCode(params.get("vnp_BankCode"));
            attempt.setCompletedAt(now);

            Payment payment = findPayment(em, order.getOrderId());
            if (success) {
                attempt.setStatus(PaymentStatus.PAID);
                if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
                    outcome = Outcome.PAID_DUPLICATE;
                } else {
                    if (payment != null) {
                        payment.setMethod(PaymentMethod.VNPAY);   // khách đã thật sự trả bằng VNPay (kể cả khi vừa đổi sang COD)
                        payment.setStatus(PaymentStatus.PAID);
                        payment.setPaidAt(now);
                    }
                    outcome = order.getStatus() == OrderStatus.CANCELLED
                            ? Outcome.PAID_ORDER_CANCELLED : Outcome.PAID;
                }
            } else {
                attempt.setStatus(PaymentStatus.FAILED);
                if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                }
                outcome = Outcome.FAILED;
            }
            trans.commit();
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể ghi nhận kết quả thanh toán", e);
        } finally {
            em.close();
        }

        String code = "#" + order.getShortCode();
        if (outcome == Outcome.PAID) {
            notifyCustomer(order, "Thanh toán VNPay thành công cho đơn hàng " + code);
        } else if (outcome == Outcome.FAILED) {
            notifyCustomer(order, "Thanh toán VNPay cho đơn hàng " + code
                    + " không thành công; bạn có thể thanh toán lại hoặc chọn thanh toán khi nhận hàng");
        } else {
            notifyCustomer(order, "Cửa hàng đã nhận tiền VNPay cho đơn hàng " + code
                    + " nhưng không thể áp dụng cho đơn; cửa hàng sẽ liên hệ để hoàn tiền");
        }
        return new Result(outcome, attempt, order);
    }

    /** Khách đổi từ VNPay sang thanh toán khi nhận hàng (COD) vì thanh toán online lỗi (SRS 3.10). */
    public static void useCod(String accountId, String orderId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            Order order = em.find(Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !order.getAccountId().equals(accountId)) {
                throw new IllegalArgumentException("Không tìm thấy đơn hàng");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Đơn hàng đã hủy, không thể đổi phương thức thanh toán");
            }
            Payment payment = findPayment(em, orderId);
            if (payment == null || payment.getMethod() != PaymentMethod.VNPAY) {
                throw new IllegalArgumentException("Đơn hàng này không thanh toán bằng VNPay");
            }
            if (payment.getStatus() == PaymentStatus.PAID) {
                throw new IllegalArgumentException("Đơn hàng này đã được thanh toán");
            }
            payment.setMethod(PaymentMethod.COD);
            payment.setStatus(PaymentStatus.PENDING);
            trans.commit();
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể đổi phương thức thanh toán, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    public static OnlinePayment selectByTxnRef(String txnRef) {
        if (txnRef == null || txnRef.isEmpty()) {
            return null;
        }
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<OnlinePayment> q = em.createQuery(
                    "SELECT p FROM OnlinePayment p WHERE p.txnRef = :txnRef", OnlinePayment.class);
            q.setParameter("txnRef", txnRef);
            try {
                return q.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        } finally {
            em.close();
        }
    }

    /** Các lần trả tiền VNPay của một đơn, mới nhất trước. */
    public static List<OnlinePayment> selectByOrderId(String orderId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<OnlinePayment> q = em.createQuery(
                    "SELECT p FROM OnlinePayment p WHERE p.orderId = :orderId ORDER BY p.createdAt DESC",
                    OnlinePayment.class);
            q.setParameter("orderId", orderId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    // ----------------------------------------------------------------------------------

    private static Payment findPayment(EntityManager em, String orderId) {
        TypedQuery<Payment> q = em.createQuery(
                "SELECT p FROM Payment p WHERE p.order.orderId = :orderId", Payment.class);
        q.setParameter("orderId", orderId);
        q.setMaxResults(1);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static void notifyCustomer(Order order, String content) {
        Notification notification = new Notification();
        notification.setAccountId(order.getAccountId());
        notification.setType(NotificationType.ORDER_STATUS_CHANGED);
        notification.setContent(content);
        NotificationDB.insert(notification);
    }

    private static void rollback(EntityTransaction trans) {
        if (trans.isActive()) {
            trans.rollback();
        }
    }
}
