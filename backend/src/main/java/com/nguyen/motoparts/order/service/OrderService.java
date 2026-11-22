package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.aftersales.service.NotificationService;
import com.nguyen.motoparts.cart.entity.CartItem;
import com.nguyen.motoparts.cart.repository.CartItemRepository;
import com.nguyen.motoparts.cart.repository.ProductLockRepository;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.DeliveryMethod;
import com.nguyen.motoparts.common.enums.InstallationStatus;
import com.nguyen.motoparts.common.enums.NotificationType;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.enums.PaymentMethod;
import com.nguyen.motoparts.common.enums.PaymentStatus;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.order.dto.CheckoutPolicyResponse;
import com.nguyen.motoparts.order.dto.CheckoutRequest;
import com.nguyen.motoparts.order.dto.OrderDetailResponse;
import com.nguyen.motoparts.order.dto.OrderSummaryResponse;
import com.nguyen.motoparts.order.entity.InstallationRequest;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.entity.OrderItem;
import com.nguyen.motoparts.order.entity.Payment;
import com.nguyen.motoparts.order.repository.InstallationRequestRepository;
import com.nguyen.motoparts.order.repository.OrderRepository;
import com.nguyen.motoparts.order.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Đặt hàng và xem đơn của khách (SRS 3.10, 3.11, 3.12).
 *
 * Đặt hàng chạy trong MỘT giao dịch: khóa tài khoản, khóa từng dòng sản phẩm (theo thứ tự mã để hai đơn cùng lúc
 * không khóa chéo), kiểm tra lại còn bán được, tạo đơn + dòng hàng + thanh toán, chuyển xe sang SOLD, xóa giỏ.
 * Thứ tự khóa luôn là tài khoản, rồi sản phẩm, rồi ngày lắp đặt (giống ShoppingCartService nên không khóa chéo với giỏ).
 */
@Service
public class OrderService {

    private static final Pattern PHONE = Pattern.compile("^(0|\\+84)[0-9]{9}$");
    private static final String MOTORBIKE = "Motorbike";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InstallationRequestRepository installationRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductLockRepository productLockRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final VnpayGateway vnpayGateway;
    private final VnpayOrderExpiryService vnpayExpiry;
    private final int holdMinutes;

    public OrderService(OrderRepository orderRepository, PaymentRepository paymentRepository,
                        InstallationRequestRepository installationRepository, CartItemRepository cartItemRepository,
                        ProductRepository productRepository, ProductLockRepository productLockRepository,
                        UserRepository userRepository, NotificationService notificationService,
                        VnpayGateway vnpayGateway, VnpayOrderExpiryService vnpayExpiry,
                        @Value("${app.cart.hold-minutes:30}") int holdMinutes) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.installationRepository = installationRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.productLockRepository = productLockRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.vnpayGateway = vnpayGateway;
        this.vnpayExpiry = vnpayExpiry;
        this.holdMinutes = holdMinutes;
    }

    public CheckoutPolicyResponse policy() {
        return new CheckoutPolicyResponse(InstallationPolicy.LABOR_FEE, InstallationPolicy.MAX_PER_DAY,
                InstallationPolicy.MAX_DAYS_AHEAD, vnpayGateway.isConfigured(), vnpayExpiry.holdMinutes());
    }

    // ---------------------------------------------------------------- đặt hàng

    @Transactional
    public OrderDetailResponse placeOrder(String accountId, CheckoutRequest req) {
        // 1) Kiểm tra dữ liệu gửi lên (chưa chạm CSDL)
        String receiverName = req.receiverName().trim();
        String receiverPhone = normalizePhone(req.receiverPhone());
        DeliveryMethod delivery = req.deliveryMethod();
        String shippingAddress = null;
        if (delivery == DeliveryMethod.DELIVERY) {
            shippingAddress = req.shippingAddress() == null ? "" : req.shippingAddress().trim();
            if (shippingAddress.isEmpty()) {
                throw new BadRequestException("Vui lòng nhập địa chỉ giao hàng.");
            }
        }
        if (req.paymentMethod() == PaymentMethod.VNPAY && !vnpayGateway.isConfigured()) {
            throw new BadRequestException("Hệ thống chưa cấu hình VNPay, vui lòng chọn thanh toán khi nhận hàng (COD).");
        }
        if (!req.returnPolicyAccepted()) {
            throw new BadRequestException("Vui lòng xác nhận chính sách đổi trả để tiếp tục.");
        }
        LocalDate installDate = null;
        if (delivery == DeliveryMethod.INSTALL) {
            installDate = req.installDate();
            InstallationPolicy.validateDate(installDate, LocalDate.now());
        }

        // 2) Khóa tài khoản rồi đọc giỏ: các thao tác của một khách chạy lần lượt (đặt hàng với sửa giỏ cùng lúc)
        userRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        List<CartItem> cartItems = cartItemRepository.findByCart_AccountIdOrderByAddedAtAsc(accountId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang trống, hãy thêm sản phẩm trước khi đặt hàng.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(holdMinutes);

        Order order = new Order();
        order.setAccountId(accountId);
        order.setStatus(OrderStatus.PENDING);
        order.setDeliveryMethod(delivery);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setShippingAddress(shippingAddress);
        order.setReturnPolicyAccepted(true);
        order.setOrderDate(now);

        // 3) Khóa và kiểm tra từng sản phẩm, theo thứ tự mã sản phẩm
        boolean hasSparePart = false;
        List<CartItem> ordered = cartItems.stream().sorted(Comparator.comparing(CartItem::getProductId)).toList();
        for (CartItem item : ordered) {
            productLockRepository.lockById(item.getProductId());
            Product product = productRepository.findById(item.getProductId()).orElse(null);   // đọc sau khi khóa

            if (product == null || product.getStatus() != ProductStatus.AVAILABLE) {
                throw unavailable(product);
            }
            if (MOTORBIKE.equals(product.getTypeCode())) {
                // Hết hạn tạm giữ mà đã có khách khác giữ xe này thì không còn quyền mua
                if (item.getAddedAt() != null && item.getAddedAt().isBefore(cutoff)
                        && cartItemRepository.countHeldByOthers(product.getProductId(), accountId, cutoff) > 0) {
                    throw unavailable(product);
                }
                product.setStatus(ProductStatus.SOLD);
            } else {
                hasSparePart = true;
            }

            OrderItem line = new OrderItem();
            line.setProductId(product.getProductId());
            line.setProductName(product.getName());
            line.setUnitPrice(product.getPrice() == null ? BigDecimal.ZERO : product.getPrice());
            line.setQuantity(item.getQuantity());
            order.addItem(line);
        }

        // 4) Lắp đặt tại cửa hàng: chỉ cho đơn có phụ tùng, ngày hẹn còn chỗ; phí công cộng vào tổng đơn
        if (delivery == DeliveryMethod.INSTALL) {
            if (!hasSparePart) {
                throw new BadRequestException("Lắp đặt tại cửa hàng chỉ áp dụng cho đơn hàng có phụ tùng.");
            }
            installationRepository.lockDay(installDate.toEpochDay());   // hai khách cùng đặt một ngày: chạy lần lượt
            if (installationRepository.countByPreferredDateAndStatusNot(installDate, InstallationStatus.CANCELLED)
                    >= InstallationPolicy.MAX_PER_DAY) {
                throw new ConflictException("Ngày này đã kín lịch, vui lòng chọn ngày khác.");
            }
            InstallationRequest installation = new InstallationRequest();
            installation.setAccountId(accountId);
            installation.setPreferredDate(installDate);
            installation.setLaborFee(InstallationPolicy.LABOR_FEE);
            installation.setStatus(InstallationStatus.SCHEDULED);
            installation.setCreatedAt(now);
            order.attachInstallation(installation);
        }
        order.recalculateTotal();   // một công thức duy nhất: các dòng hàng + phí công của lịch lắp đặt còn hiệu lực
        BigDecimal total = order.getTotalAmount();
        orderRepository.save(order);

        // 5) Thanh toán (COD: chờ Admin xác nhận đã thu tiền; VNPay: khách bấm trả tiền ở bước sau), xóa giỏ, thông báo
        Payment payment = new Payment();
        payment.setOrderId(order.getOrderId());
        payment.setMethod(req.paymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(total);
        payment.setCreatedAt(now);
        paymentRepository.save(payment);

        for (CartItem item : cartItems) {
            item.getCart().removeItem(item);
            cartItemRepository.delete(item);
        }
        notificationService.notify(accountId, NotificationType.ORDER_PLACED,
                "Đơn hàng #" + order.getShortCode() + " đã được đặt thành công");

        return toDetail(order, payment);
    }

    // ---------------------------------------------------------------- xem đơn của tôi

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listMine(String accountId) {
        List<Order> orders = orderRepository.findByAccountIdOrderByOrderDateDesc(accountId);
        Map<String, Payment> payments = paymentRepository
                .findByOrderIdIn(orders.stream().map(Order::getOrderId).toList()).stream()
                .collect(Collectors.toMap(Payment::getOrderId, Function.identity()));
        return orders.stream().map(o -> toSummary(o, payments.get(o.getOrderId()))).toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMine(String accountId, String orderId) {
        Order order = orderRepository.findByOrderIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        return toDetail(order, paymentRepository.findByOrderId(orderId).orElse(null));
    }

    // ---------------------------------------------------------------- phụ trợ

    static OrderSummaryResponse toSummary(Order o, Payment p) {
        int itemCount = o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        return new OrderSummaryResponse(o.getOrderId(), o.getShortCode(), o.getOrderDate(),
                o.getStatus().name(), o.getStatus().getLabel(), o.getTotalAmount(), itemCount,
                p == null ? null : p.getMethod().name(), p == null ? null : p.getMethod().getLabel(),
                p == null ? null : p.getStatus().name(), p == null ? null : p.getStatus().getLabel());
    }

    static OrderDetailResponse toDetail(Order o, Payment p) {
        List<OrderDetailResponse.Item> items = o.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getProductName, Comparator.nullsLast(String::compareTo)))
                .map(i -> new OrderDetailResponse.Item(i.getProductId(), i.getProductName(), i.getUnitPrice(),
                        i.getQuantity(), i.getLineTotal()))
                .toList();
        OrderDetailResponse.PaymentInfo payment = p == null ? null : new OrderDetailResponse.PaymentInfo(
                p.getMethod().name(), p.getMethod().getLabel(), p.getStatus().name(), p.getStatus().getLabel(),
                p.getAmount(), p.getPaidAt(),
                p.getMethod() == PaymentMethod.VNPAY && p.getStatus() != PaymentStatus.PAID
                        && o.getStatus() != OrderStatus.CANCELLED);
        InstallationRequest ir = o.getInstallationRequest();
        OrderDetailResponse.InstallationInfo installation = ir == null ? null : new OrderDetailResponse.InstallationInfo(
                ir.getPreferredDate(), ir.getLaborFee(), ir.getStatus().name(), ir.getStatus().getLabel());
        return new OrderDetailResponse(o.getOrderId(), o.getShortCode(), o.getOrderDate(),
                o.getStatus().name(), o.getStatus().getLabel(),
                o.getDeliveryMethod() == null ? null : o.getDeliveryMethod().name(),
                o.getDeliveryMethod() == null ? null : o.getDeliveryMethod().getLabel(),
                o.getReceiverName(), o.getReceiverPhone(), o.getShippingAddress(), o.getTotalAmount(),
                o.isReturnPolicyAccepted(), items, payment, installation);
    }

    private static ConflictException unavailable(Product product) {
        String name = product == null ? "Một sản phẩm" : "\"" + product.getName() + "\"";
        return new ConflictException(name + " không còn khả dụng, vui lòng kiểm tra lại giỏ hàng.");
    }

    /** Bỏ khoảng trắng và dấu . ( ) - rồi kiểm tra dạng 0xxxxxxxxx hoặc +84xxxxxxxxx (giống sổ địa chỉ). */
    private static String normalizePhone(String phone) {
        String normalized = phone.trim().replaceAll("[ .()-]", "");
        if (!PHONE.matcher(normalized).matches()) {
            throw new BadRequestException("Số điện thoại không hợp lệ.");
        }
        return normalized;
    }
}
