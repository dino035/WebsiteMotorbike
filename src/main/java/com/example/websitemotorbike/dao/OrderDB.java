package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.CartItem;
import com.example.websitemotorbike.model.DeliveryMethod;
import com.example.websitemotorbike.model.InstallationRequest;
import com.example.websitemotorbike.model.InstallationStatus;
import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.Notification;
import com.example.websitemotorbike.model.NotificationType;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.model.OrderItem;
import com.example.websitemotorbike.model.OrderStatus;
import com.example.websitemotorbike.model.Payment;
import com.example.websitemotorbike.model.PaymentMethod;
import com.example.websitemotorbike.model.PaymentStatus;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.ProductStatus;
import com.example.websitemotorbike.util.DBUtil;
import com.example.websitemotorbike.util.InstallationPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDB {

    /**
     * Đặt hàng từ giỏ của accountId trong MỘT giao dịch: khóa từng dòng sản phẩm, kiểm tra lại
     * còn bán được không, tạo Order + OrderItem + Payment, chuyển xe sang SOLD, rồi xóa các dòng giỏ.
     *
     * Hình thức INSTALL (lắp đặt tại cửa hàng): đơn phải có phụ tùng, ngày hẹn còn lịch trống;
     * phí công được cộng vào tổng đơn và tạo InstallationRequest trong cùng giao dịch (SRS 3.11).
     *
     * @param installDate ngày hẹn lắp đặt, chỉ dùng khi deliveryMethod là INSTALL (các trường hợp khác để null)
     * @throws IllegalArgumentException giỏ trống, sản phẩm không còn khả dụng hoặc ngày kín lịch (đã rollback)
     * @throws IllegalStateException    lỗi CSDL (đã rollback)
     */
    public static Order placeOrder(String accountId, DeliveryMethod deliveryMethod,
                                   String receiverName, String receiverPhone, String shippingAddress,
                                   PaymentMethod paymentMethod, boolean returnPolicyAccepted,
                                   LocalDate installDate) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        Order order;
        try {
            TypedQuery<CartItem> q = em.createQuery(
                    "SELECT i FROM CartItem i WHERE i.cart.accountId = :accountId ORDER BY i.addedAt",
                    CartItem.class);
            q.setParameter("accountId", accountId);
            List<CartItem> cartItems = q.getResultList();
            if (cartItems.isEmpty()) {
                throw new IllegalArgumentException("Giỏ hàng của bạn đang trống");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoff = now.minusMinutes(CartDB.HOLD_MINUTES);

            order = new Order();
            order.setAccountId(accountId);
            order.setStatus(OrderStatus.PENDING);
            order.setDeliveryMethod(deliveryMethod);
            order.setReceiverName(receiverName);
            order.setReceiverPhone(receiverPhone);
            order.setShippingAddress(shippingAddress);
            order.setReturnPolicyAccepted(returnPolicyAccepted);
            order.setCreatedAt(now);
            order.setTotalAmount(BigDecimal.ZERO);
            em.persist(order);

            BigDecimal total = BigDecimal.ZERO;
            boolean hasSparePart = false;
            for (CartItem item : cartItems) {
                Product product = lockAndLoad(em, item.getProductId());

                if (product == null || product.getStatus() != ProductStatus.AVAILABLE) {
                    throw unavailable(product);
                }
                if (product instanceof Motorbike) {
                    if (item.getAddedAt().isBefore(cutoff)
                            && CartDB.countHeldByOthers(em, item.getProductId(), accountId, cutoff) > 0) {
                        throw unavailable(product);
                    }
                    product.setStatus(ProductStatus.SOLD);
                } else {
                    hasSparePart = true;
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProductId(product.getProductId());
                orderItem.setProductName(product.getName());
                orderItem.setUnitPrice(product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
                orderItem.setQuantity(item.getQuantity());
                em.persist(orderItem);

                total = total.add(orderItem.getLineTotal());
                em.remove(item);
            }
            if (deliveryMethod == DeliveryMethod.INSTALL) {
                if (!hasSparePart) {
                    throw new IllegalArgumentException(
                            "Lắp đặt tại cửa hàng chỉ áp dụng cho đơn hàng có phụ tùng");
                }
                if (installDate == null) {
                    throw new IllegalArgumentException("Vui lòng chọn ngày hẹn lắp đặt");
                }
                InstallationPolicy.validateDate(installDate, LocalDate.now());

                // Khóa theo ngày rồi đếm, để hai khách cùng đặt một ngày không vượt số lượt tối đa
                InstallationDB.lockDay(em, installDate);
                if (InstallationDB.countActiveOnDate(em, installDate) >= InstallationPolicy.MAX_PER_DAY) {
                    throw new IllegalArgumentException("Ngày này đã kín lịch, vui lòng chọn ngày khác");
                }

                total = total.add(InstallationPolicy.LABOR_FEE);   // cộng phí công vào tổng đơn
                InstallationRequest installation = new InstallationRequest();
                installation.setAccountId(accountId);
                installation.setOrder(order);
                installation.setScheduledDate(installDate);
                installation.setLaborFee(InstallationPolicy.LABOR_FEE);
                installation.setStatus(InstallationStatus.SCHEDULED);
                installation.setCreatedAt(now);
                em.persist(installation);
            }
            order.setTotalAmount(total);

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setMethod(paymentMethod);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmount(total);
            payment.setCreatedAt(now);
            em.persist(payment);

            trans.commit();
        } catch (IllegalArgumentException e) {
            if (trans.isActive()) {
                trans.rollback();
            }
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể đặt hàng, vui lòng thử lại", e);
        } finally {
            em.close();
        }

        // Tạo thông báo qua DAO của Nhóm 2 (README mục 3, quy tắc 7); lỗi ở đây không ảnh hưởng đơn hàng
        Notification notification = new Notification();
        notification.setAccountId(accountId);
        notification.setType(NotificationType.ORDER_PLACED);
        notification.setContent("Đơn hàng #" + order.getShortCode() + " đã được đặt thành công");
        NotificationDB.insert(notification);

        return order;
    }

    public static List<Order> selectByAccountId(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Order> q = em.createQuery(
                    "SELECT o FROM Orders o WHERE o.accountId = :accountId ORDER BY o.createdAt DESC",
                    Order.class);
            q.setParameter("accountId", accountId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Chỉ trả đơn nếu thuộc về accountId (chống xem đơn của người khác). */
    public static Order selectOwned(String accountId, String orderId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Order> q = em.createQuery(
                    "SELECT o FROM Orders o WHERE o.orderId = :orderId AND o.accountId = :accountId",
                    Order.class);
            q.setParameter("orderId", orderId);
            q.setParameter("accountId", accountId);
            try {
                return q.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        } finally {
            em.close();
        }
    }

    public static List<OrderItem> selectItems(String orderId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<OrderItem> q = em.createQuery(
                    "SELECT i FROM OrderItem i WHERE i.order.orderId = :orderId ORDER BY i.productName",
                    OrderItem.class);
            q.setParameter("orderId", orderId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public static Payment selectPayment(String orderId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Payment> q = em.createQuery(
                    "SELECT p FROM Payment p WHERE p.order.orderId = :orderId", Payment.class);
            q.setParameter("orderId", orderId);
            q.setMaxResults(1);
            try {
                return q.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        } finally {
            em.close();
        }
    }

    // ----------------------------------------------------------------------------------
    // Dành cho Admin (use case 3.21)
    // ----------------------------------------------------------------------------------

    /** Mọi đơn hàng, mới nhất trước; status = null thì không lọc theo trạng thái. */
    public static List<Order> selectAll(OrderStatus status) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            String jpql = "SELECT o FROM Orders o"
                    + (status == null ? "" : " WHERE o.status = :status")
                    + " ORDER BY o.createdAt DESC";
            TypedQuery<Order> q = em.createQuery(jpql, Order.class);
            if (status != null) {
                q.setParameter("status", status);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public static Order selectById(String orderId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return em.find(Order.class, orderId);
        } finally {
            em.close();
        }
    }

    /**
     * Admin chuyển trạng thái đơn hàng. Chỉ cho phép các bước hợp lệ (OrderStatus.canMoveTo).
     * Hoàn thành đơn yêu cầu đã thu tiền; hủy đơn yêu cầu chưa thu tiền và trả các xe trong đơn
     * về AVAILABLE. Mọi thay đổi nằm trong một giao dịch; sau khi lưu sẽ thông báo cho khách.
     */
    public static Order updateStatus(String orderId, OrderStatus newStatus) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        Order order;
        try {
            order = em.find(Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) {
                throw new IllegalArgumentException("Không tìm thấy đơn hàng");
            }
            OrderStatus current = order.getStatus();
            if (!current.canMoveTo(newStatus)) {
                throw new IllegalArgumentException("Không thể chuyển đơn từ \"" + current.getLabel()
                        + "\" sang \"" + newStatus.getLabel() + "\"");
            }

            Payment payment = findPayment(em, orderId);
            boolean paid = payment != null && payment.getStatus() == PaymentStatus.PAID;

            if (newStatus == OrderStatus.COMPLETED && !paid) {
                throw new IllegalArgumentException(
                        "Hãy xác nhận đã thu tiền trước khi hoàn thành đơn hàng");
            }
            if (newStatus == OrderStatus.CANCELLED) {
                if (paid) {
                    throw new IllegalArgumentException("Đơn hàng đã thu tiền, không thể hủy");
                }
                releaseMotorbikes(em, orderId);
                InstallationDB.cancelForOrder(em, orderId);   // hủy lịch lắp đặt, trả chỗ trống
            }

            order.setStatus(newStatus);
            trans.commit();
        } catch (IllegalArgumentException e) {
            if (trans.isActive()) {
                trans.rollback();
            }
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể cập nhật đơn hàng, vui lòng thử lại", e);
        } finally {
            em.close();
        }

        notifyCustomer(order, "Đơn hàng #" + order.getShortCode() + " đã chuyển sang trạng thái: "
                + order.getStatus().getLabel());
        return order;
    }

    /** Admin xác nhận đã thu tiền COD: Payment chuyển sang PAID và ghi paidAt. */
    public static Order confirmPayment(String orderId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        Order order;
        try {
            order = em.find(Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) {
                throw new IllegalArgumentException("Không tìm thấy đơn hàng");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Đơn hàng đã hủy, không thể xác nhận thu tiền");
            }
            Payment payment = findPayment(em, orderId);
            if (payment == null) {
                throw new IllegalArgumentException("Đơn hàng chưa có thông tin thanh toán");
            }
            if (payment.getMethod() != PaymentMethod.COD) {
                throw new IllegalArgumentException("Chỉ xác nhận thu tiền thủ công cho đơn COD");
            }
            if (payment.getStatus() == PaymentStatus.PAID) {
                throw new IllegalArgumentException("Đơn hàng này đã được xác nhận thu tiền");
            }
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            trans.commit();
        } catch (IllegalArgumentException e) {
            if (trans.isActive()) {
                trans.rollback();
            }
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể cập nhật đơn hàng, vui lòng thử lại", e);
        } finally {
            em.close();
        }

        notifyCustomer(order, "Đã xác nhận thu tiền cho đơn hàng #" + order.getShortCode());
        return order;
    }

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

    /** Khi hủy đơn: các xe đã bị đặt sang SOLD được trả về AVAILABLE (ngoại lệ 2.1 của use case 3.21). */
    private static void releaseMotorbikes(EntityManager em, String orderId) {
        TypedQuery<OrderItem> q = em.createQuery(
                "SELECT i FROM OrderItem i WHERE i.order.orderId = :orderId", OrderItem.class);
        q.setParameter("orderId", orderId);
        for (OrderItem item : q.getResultList()) {
            Product product = lockAndLoad(em, item.getProductId());
            if (product instanceof Motorbike && product.getStatus() == ProductStatus.SOLD) {
                product.setStatus(ProductStatus.AVAILABLE);
            }
        }
    }

    /** Tạo thông báo cho khách qua DAO của Nhóm 2; lỗi thông báo không làm hỏng thao tác chính. */
    private static void notifyCustomer(Order order, String content) {
        Notification notification = new Notification();
        notification.setAccountId(order.getAccountId());
        notification.setType(NotificationType.ORDER_STATUS_CHANGED);
        notification.setContent(content);
        NotificationDB.insert(notification);
    }

    /**
     * Khóa dòng sản phẩm (SELECT ... FOR UPDATE) để hai đơn không cùng bán một chiếc xe,
     * rồi đọc lại trạng thái mới nhất từ CSDL.
     */
    private static Product lockAndLoad(EntityManager em, String productId) {
        em.createNativeQuery("SELECT productid FROM product WHERE productid = ?1 FOR UPDATE")
                .setParameter(1, productId)
                .getResultList();
        Product product = em.find(Product.class, productId);
        if (product != null) {
            em.refresh(product);
        }
        return product;
    }

    private static IllegalArgumentException unavailable(Product product) {
        String name = product == null ? "Một sản phẩm" : "\"" + product.getName() + "\"";
        return new IllegalArgumentException(name + " không còn khả dụng, vui lòng kiểm tra lại giỏ hàng");
    }
}
