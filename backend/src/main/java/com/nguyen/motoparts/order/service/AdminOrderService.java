package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.aftersales.service.NotificationService;
import com.nguyen.motoparts.cart.repository.ProductLockRepository;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.common.enums.InstallationStatus;
import com.nguyen.motoparts.common.enums.NotificationType;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.enums.PaymentMethod;
import com.nguyen.motoparts.common.enums.PaymentStatus;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.order.dto.AdminOrderDetail;
import com.nguyen.motoparts.order.dto.AdminOrderRow;
import com.nguyen.motoparts.order.entity.InstallationRequest;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.entity.OrderItem;
import com.nguyen.motoparts.order.entity.Payment;
import com.nguyen.motoparts.order.repository.OnlinePaymentRepository;
import com.nguyen.motoparts.order.repository.OrderRepository;
import com.nguyen.motoparts.order.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin quản lý đơn hàng (SRS 3.21): xem danh sách và chi tiết, đổi trạng thái, xác nhận đã thu tiền COD.
 *
 * Mọi thao tác ghi khóa dòng đơn (SELECT ... FOR UPDATE) trước. Hoàn thành đơn cần đã thu tiền; hủy đơn cần chưa thu tiền
 * và trả các xe trong đơn về AVAILABLE, hủy luôn lịch lắp đặt để trả chỗ trống. Sau khi lưu có thông báo cho khách.
 */
@Service
public class AdminOrderService {

    private static final String MOTORBIKE = "Motorbike";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductLockRepository productLockRepository;
    private final NotificationService notificationService;
    private final OnlinePaymentRepository onlinePaymentRepository;

    public AdminOrderService(OrderRepository orderRepository, PaymentRepository paymentRepository,
                             UserRepository userRepository, ProductRepository productRepository,
                             ProductLockRepository productLockRepository, NotificationService notificationService,
                             OnlinePaymentRepository onlinePaymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productLockRepository = productLockRepository;
        this.notificationService = notificationService;
        this.onlinePaymentRepository = onlinePaymentRepository;
    }

    // ---------------------------------------------------------------- xem

    /** Mọi đơn, mới nhất trước; statusText trống thì không lọc theo trạng thái. */
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderRow> list(String statusText, int page, int size) {
        OrderStatus status = parseStatus(statusText);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Order> orders = status == null
                ? orderRepository.findAllByOrderByOrderDateDesc(pageable)
                : orderRepository.findByStatusOrderByOrderDateDesc(status, pageable);

        Map<String, Payment> payments = paymentRepository
                .findByOrderIdIn(orders.stream().map(Order::getOrderId).toList()).stream()
                .collect(Collectors.toMap(Payment::getOrderId, Function.identity()));
        Map<String, String> emails = userRepository
                .findAllById(orders.stream().map(Order::getAccountId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getAccountId, User::getEmail));

        return PageResponse.from(orders.map(o -> {
            Payment p = payments.get(o.getOrderId());
            return new AdminOrderRow(o.getOrderId(), o.getShortCode(), o.getOrderDate(),
                    o.getStatus().name(), o.getStatus().getLabel(), o.getTotalAmount(),
                    emails.get(o.getAccountId()), o.getReceiverName(),
                    o.getDeliveryMethod() == null ? null : o.getDeliveryMethod().getLabel(),   // đơn nhập tay/cũ có thể thiếu
                    p == null ? null : p.getMethod().name(), p == null ? null : p.getMethod().getLabel(),
                    p == null ? null : p.getStatus().name(), p == null ? null : p.getStatus().getLabel());
        }));
    }

    @Transactional(readOnly = true)
    public AdminOrderDetail detail(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        return toDetail(order, paymentRepository.findByOrderId(orderId).orElse(null));
    }

    // ---------------------------------------------------------------- ghi

    @Transactional
    public AdminOrderDetail changeStatus(String orderId, OrderStatus newStatus) {
        Order order = lockOrder(orderId);
        OrderStatus current = order.getStatus();
        if (!current.canMoveTo(newStatus)) {
            throw new ConflictException("Không thể chuyển đơn từ \"" + current.getLabel()
                    + "\" sang \"" + newStatus.getLabel() + "\".");
        }
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        boolean paid = payment != null && payment.getStatus() == PaymentStatus.PAID;

        if (newStatus == OrderStatus.COMPLETED && !paid) {
            throw new ConflictException("Hãy xác nhận đã thu tiền trước khi hoàn thành đơn hàng.");
        }
        if (newStatus == OrderStatus.CANCELLED) {
            if (paid) {
                throw new ConflictException("Đơn hàng đã thu tiền, không thể hủy.");
            }
            releaseMotorbikes(order);
            InstallationRequest installation = order.getInstallationRequest();
            if (installation != null && installation.getStatus() == InstallationStatus.SCHEDULED) {
                installation.setStatus(InstallationStatus.CANCELLED);   // trả chỗ trống của ngày hẹn
            }
        }
        order.setStatus(newStatus);
        notificationService.notify(order.getAccountId(), NotificationType.ORDER_STATUS_CHANGED,
                "Đơn hàng #" + order.getShortCode() + " đã chuyển sang trạng thái: " + newStatus.getLabel());
        return toDetail(order, payment);
    }

    /** Admin xác nhận đã thu tiền COD: thanh toán chuyển PAID và ghi paidAt. */
    @Transactional
    public AdminOrderDetail confirmPayment(String orderId) {
        Order order = lockOrder(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Đơn hàng đã hủy, không thể xác nhận thu tiền.");
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ConflictException("Đơn hàng chưa có thông tin thanh toán."));
        if (payment.getMethod() != PaymentMethod.COD) {
            throw new ConflictException("Chỉ xác nhận thu tiền thủ công cho đơn COD.");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Đơn hàng này đã được xác nhận thu tiền.");
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        notificationService.notify(order.getAccountId(), NotificationType.ORDER_STATUS_CHANGED,
                "Đã xác nhận thu tiền cho đơn hàng #" + order.getShortCode());
        return toDetail(order, payment);
    }

    // ---------------------------------------------------------------- phụ trợ

    private Order lockOrder(String orderId) {
        return orderRepository.findForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
    }

    /** Khi hủy đơn: các xe đã bị đặt sang SOLD được trả về AVAILABLE (ngoại lệ 2.1 của use case 3.21). */
    private void releaseMotorbikes(Order order) {
        List<OrderItem> items = order.getItems().stream().sorted(Comparator.comparing(OrderItem::getProductId)).toList();
        for (OrderItem item : items) {
            productLockRepository.lockById(item.getProductId());
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null && MOTORBIKE.equals(product.getTypeCode()) && product.getStatus() == ProductStatus.SOLD) {
                product.setStatus(ProductStatus.AVAILABLE);
            }
        }
    }

    private AdminOrderDetail toDetail(Order order, Payment payment) {
        User user = userRepository.findById(order.getAccountId()).orElse(null);
        AdminOrderDetail.Customer customer = new AdminOrderDetail.Customer(order.getAccountId(),
                user == null ? null : user.getEmail(), user == null ? null : user.getPhone());
        List<AdminOrderDetail.NextStatus> next = order.getStatus().getNextStatuses().stream()
                .map(s -> new AdminOrderDetail.NextStatus(s.name(), s.getLabel())).toList();
        boolean canConfirm = payment != null && payment.getMethod() == PaymentMethod.COD
                && payment.getStatus() == PaymentStatus.PENDING && order.getStatus() != OrderStatus.CANCELLED;
        List<AdminOrderDetail.OnlineAttempt> attempts = onlinePaymentRepository
                .findByOrderIdOrderByCreatedAtDesc(order.getOrderId()).stream()
                .map(a -> new AdminOrderDetail.OnlineAttempt(a.getTxnRef(), a.getAmount(), a.getStatus().name(),
                        a.getStatus().getLabel(), a.getResponseCode(), a.getTransactionNo(), a.getBankCode(),
                        a.getCreatedAt(), a.getCompletedAt()))
                .toList();
        return new AdminOrderDetail(OrderService.toDetail(order, payment), customer, next, canConfirm, attempts);
    }

    private static OrderStatus parseStatus(String text) {
        if (text == null || text.isBlank() || "ALL".equalsIgnoreCase(text.trim())) {
            return null;
        }
        try {
            return OrderStatus.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái không hợp lệ.");
        }
    }
}
