package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.aftersales.service.NotificationService;
import com.nguyen.motoparts.common.enums.NotificationType;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.enums.PaymentMethod;
import com.nguyen.motoparts.common.enums.PaymentStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.order.dto.OrderDetailResponse;
import com.nguyen.motoparts.order.dto.PaymentLinkResponse;
import com.nguyen.motoparts.order.entity.OnlinePayment;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.entity.Payment;
import com.nguyen.motoparts.order.repository.OnlinePaymentRepository;
import com.nguyen.motoparts.order.repository.OrderRepository;
import com.nguyen.motoparts.order.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Thanh toán trực tuyến qua VNPay Sandbox (SRS 3.10). Mỗi lần khách bấm trả tiền là một bản ghi OnlinePayment với
 * mã giao dịch (vnp_TxnRef) mới; một đơn có thể thử lại nhiều lần.
 *
 * Mọi thay đổi khóa dòng đơn trước (SELECT ... FOR UPDATE, giống AdminOrderService) nên việc trả tiền, hủy đơn và đổi
 * sang COD không giẫm lên nhau. Kết quả được ghi nhận qua cả hai đường: trình duyệt của khách quay về (return) và VNPay
 * gọi thẳng máy chủ (IPN, cần địa chỉ công khai nên chạy localhost thì chỉ có đường return). Hai đường dùng chung
 * {@link #process}, chạy lặp lại không ghi trùng.
 */
@Service
public class OnlinePaymentService {

    /** Kết quả xử lý một thông báo kết quả từ VNPay. */
    public enum Outcome {
        /** Chưa cấu hình VNPay trên hệ thống. */
        NOT_CONFIGURED,
        /** Chữ ký sai hoặc không đúng mã website: không ghi nhận gì. */
        INVALID_SIGNATURE,
        /** Không có giao dịch nào mang mã này. */
        NOT_FOUND,
        /** Số tiền VNPay báo khác số tiền của giao dịch: không ghi nhận gì. */
        AMOUNT_MISMATCH,
        /** Giao dịch đã được xử lý trước đó (trình duyệt quay lại và IPN đến hai lần). */
        ALREADY_PROCESSED,
        /** Trả tiền thành công, đơn chuyển sang đã thanh toán. */
        PAID,
        /** Trả tiền thành công nhưng đơn đã bị hủy trước đó: cần hoàn tiền thủ công. */
        PAID_ORDER_CANCELLED,
        /** Trả tiền thành công nhưng đơn đã được thanh toán bằng lần khác: cần hoàn tiền thủ công. */
        PAID_DUPLICATE,
        /** Khách hủy, hết hạn hoặc ngân hàng từ chối. */
        FAILED
    }

    /** attemptPaid: lần trả tiền này đã được ghi nhận là thành công (kể cả khi nó đã được xử lý từ trước). */
    public record Result(Outcome outcome, String orderId, String orderCode, String reason, boolean attemptPaid) {
    }

    private final OnlinePaymentRepository attempts;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final VnpayGateway gateway;

    public OnlinePaymentService(OnlinePaymentRepository attempts, OrderRepository orderRepository,
                                PaymentRepository paymentRepository, NotificationService notificationService,
                                VnpayGateway gateway) {
        this.attempts = attempts;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.gateway = gateway;
    }

    // ---------------------------------------------------------------- khách bắt đầu trả tiền

    /** Tạo một lần trả tiền VNPay cho đơn của accountId và trả liên kết sang cổng thanh toán. */
    @Transactional
    public PaymentLinkResponse startAttempt(String accountId, String orderId, String ipAddress) {
        if (!gateway.isConfigured()) {
            throw new BadRequestException("Hệ thống chưa cấu hình VNPay, vui lòng chọn thanh toán khi nhận hàng (COD).");
        }
        Order order = ownedOrder(accountId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Đơn hàng đã hủy, không thể thanh toán.");
        }
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null || payment.getMethod() != PaymentMethod.VNPAY) {
            throw new ConflictException("Đơn hàng này không thanh toán bằng VNPay.");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Đơn hàng này đã được thanh toán.");
        }
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
            throw new ConflictException("Số tiền thanh toán không hợp lệ.");
        }

        OnlinePayment attempt = new OnlinePayment();
        attempt.setOrderId(orderId);
        attempt.setAccountId(accountId);
        attempt.setTxnRef(UUID.randomUUID().toString().replace("-", ""));
        attempt.setAmount(payment.getAmount());
        attempt.setStatus(PaymentStatus.PENDING);
        attempt.setCreatedAt(LocalDateTime.now());
        attempts.save(attempt);
        if (payment.getStatus() == PaymentStatus.FAILED) {
            payment.setStatus(PaymentStatus.PENDING);   // đang thử lại
        }
        return new PaymentLinkResponse(gateway.buildPaymentUrl(attempt, order.getShortCode(), ipAddress),
                attempt.getTxnRef());
    }

    /** Khách đổi từ VNPay sang thanh toán khi nhận hàng (COD) vì thanh toán trực tuyến lỗi. */
    @Transactional
    public OrderDetailResponse useCod(String accountId, String orderId) {
        Order order = ownedOrder(accountId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Đơn hàng đã hủy, không thể đổi phương thức thanh toán.");
        }
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null || payment.getMethod() != PaymentMethod.VNPAY) {
            throw new ConflictException("Đơn hàng này không thanh toán bằng VNPay.");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Đơn hàng này đã được thanh toán.");
        }
        payment.setMethod(PaymentMethod.COD);
        payment.setStatus(PaymentStatus.PENDING);   // chờ Admin xác nhận đã thu tiền
        return OrderService.toDetail(order, payment);
    }

    // ---------------------------------------------------------------- ghi nhận kết quả từ VNPay

    /** Kiểm tra chữ ký rồi ghi nhận kết quả (dùng cho cả đường return lẫn IPN). */
    @Transactional
    public Result process(Map<String, String> params) {
        if (!gateway.isConfigured()) {
            return new Result(Outcome.NOT_CONFIGURED, null, null, null, false);
        }
        if (!gateway.verify(params)) {
            return new Result(Outcome.INVALID_SIGNATURE, null, null, null, false);
        }
        return complete(params);
    }

    /** Ghi nhận kết quả; chỉ gọi SAU KHI đã kiểm tra chữ ký. Chạy lặp lại không làm gì thêm. */
    private Result complete(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");
        Optional<String> orderId = txnRef == null || txnRef.isEmpty() ? Optional.empty() : attempts.findOrderIdByTxnRef(txnRef);
        // khóa đơn rồi mới đọc giao dịch: hai thông báo cùng lúc chạy lần lượt và cái sau thấy trạng thái mới
        Order order = orderId.flatMap(orderRepository::findForUpdate).orElse(null);
        if (order == null) {
            return new Result(Outcome.NOT_FOUND, null, null, null, false);
        }
        OnlinePayment attempt = attempts.findByTxnRef(txnRef).orElseThrow();
        String reason = VnpayGateway.describeResponse(params.get("vnp_ResponseCode"));

        if (attempt.getStatus() != PaymentStatus.PENDING) {
            return result(Outcome.ALREADY_PROCESSED, order, reason, attempt.getStatus() == PaymentStatus.PAID);
        }
        if (!String.valueOf(VnpayGateway.toVnpAmount(attempt.getAmount())).equals(params.get("vnp_Amount"))) {
            return result(Outcome.AMOUNT_MISMATCH, order, reason, false);
        }

        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && (transactionStatus == null || transactionStatus.isEmpty() || "00".equals(transactionStatus));

        LocalDateTime now = LocalDateTime.now();
        attempt.setResponseCode(params.get("vnp_ResponseCode"));
        attempt.setTransactionNo(params.get("vnp_TransactionNo"));
        attempt.setBankCode(params.get("vnp_BankCode"));
        attempt.setCompletedAt(now);

        Payment payment = paymentRepository.findByOrderId(order.getOrderId()).orElse(null);
        Outcome outcome;
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
                outcome = order.getStatus() == OrderStatus.CANCELLED ? Outcome.PAID_ORDER_CANCELLED : Outcome.PAID;
            }
        } else {
            attempt.setStatus(PaymentStatus.FAILED);
            if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
            }
            outcome = Outcome.FAILED;
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
        return result(outcome, order, reason, success);
    }

    // ---------------------------------------------------------------- phụ trợ

    private Order ownedOrder(String accountId, String orderId) {
        return orderRepository.findForUpdate(orderId)
                .filter(o -> o.getAccountId().equals(accountId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
    }

    private static Result result(Outcome outcome, Order order, String reason, boolean attemptPaid) {
        return new Result(outcome, order.getOrderId(), order.getShortCode(), reason, attemptPaid);
    }

    private void notifyCustomer(Order order, String content) {
        notificationService.notify(order.getAccountId(), NotificationType.ORDER_STATUS_CHANGED, content);
    }
}
