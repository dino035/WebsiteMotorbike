package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.aftersales.service.NotificationService;
import com.nguyen.motoparts.common.enums.NotificationType;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.enums.PaymentMethod;
import com.nguyen.motoparts.common.enums.PaymentStatus;
import com.nguyen.motoparts.order.entity.OnlinePayment;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.entity.Payment;
import com.nguyen.motoparts.order.repository.OnlinePaymentRepository;
import com.nguyen.motoparts.order.repository.OrderRepository;
import com.nguyen.motoparts.order.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự hủy đơn VNPay không được trả tiền (SRS 3.10): đơn chọn VNPay mà sau {@code holdMinutes} phút vẫn chưa thanh toán thì
 * bị hủy để xe được mở bán lại (không thì xe kẹt ở trạng thái đã bán).
 *
 * Một đơn chỉ bị hủy khi đủ ba điều kiện, kiểm tra LẠI sau khi khóa dòng đơn (SELECT ... FOR UPDATE, cùng khóa với việc
 * trả tiền, đổi sang COD và Admin đổi trạng thái nên không giẫm lên nhau):
 * <ul>
 *   <li>vẫn đang chờ xác nhận, thanh toán vẫn là VNPay và chưa PAID (khách đổi sang COD hay đã trả thì không đụng tới);</li>
 *   <li>đã đặt quá {@code holdMinutes} phút;</li>
 *   <li>không còn liên kết thanh toán nào còn hiệu lực: khách thử lại ở phút thứ 14 thì được trả tiếp hết hạn liên kết đó
 *       (cộng thêm {@link #GRACE_MINUTES} phút cho VNPay gọi IPN), tránh hủy đơn đúng lúc khách đang nhập OTP.</li>
 * </ul>
 */
@Service
public class VnpayOrderExpiryService {

    /** Chờ thêm sau khi liên kết hết hạn, để kết quả cuối cùng của VNPay kịp về. */
    static final int GRACE_MINUTES = 2;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OnlinePaymentRepository attempts;
    private final OrderStockRelease stockRelease;
    private final NotificationService notificationService;
    private final VnpayGateway gateway;
    private final int holdMinutes;

    public VnpayOrderExpiryService(OrderRepository orderRepository, PaymentRepository paymentRepository,
                                   OnlinePaymentRepository attempts, OrderStockRelease stockRelease,
                                   NotificationService notificationService, VnpayGateway gateway,
                                   @Value("${app.vnpay.order-hold-minutes:15}") int holdMinutes) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.attempts = attempts;
        this.stockRelease = stockRelease;
        this.notificationService = notificationService;
        this.gateway = gateway;
        this.holdMinutes = holdMinutes >= 5 && holdMinutes <= 1440 ? holdMinutes : 15;
    }

    /** Số phút khách có để trả tiền một đơn VNPay (sau đó đơn tự hủy), React dùng để nhắc khách. */
    public int holdMinutes() {
        return holdMinutes;
    }

    /** Danh sách ứng viên (chưa khóa): đơn VNPay chưa trả tiền đã đặt quá holdMinutes phút tính đến now. */
    @Transactional(readOnly = true)
    public List<String> findCandidates(LocalDateTime now) {
        return orderRepository.findUnpaidOrderIds(OrderStatus.PENDING, PaymentMethod.VNPAY, PaymentStatus.PAID,
                now.minusMinutes(holdMinutes));
    }

    /** Khóa đơn, kiểm tra lại điều kiện rồi hủy. Trả true nếu đơn đã bị hủy ở lần gọi này. */
    @Transactional
    public boolean expireIfStillUnpaid(String orderId, LocalDateTime now) {
        Order order = orderRepository.findForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING || order.getOrderDate() == null) {
            return false;
        }
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null || payment.getMethod() != PaymentMethod.VNPAY || payment.getStatus() == PaymentStatus.PAID) {
            return false;
        }
        if (order.getOrderDate().plusMinutes(holdMinutes).isAfter(now)) {
            return false;   // chưa tới hạn
        }
        List<OnlinePayment> history = attempts.findByOrderIdOrderByCreatedAtDesc(orderId);   // đọc sau khi khóa đơn
        if (!history.isEmpty()
                && history.get(0).getCreatedAt().plusMinutes(gateway.expireMinutes() + GRACE_MINUTES).isAfter(now)) {
            return false;   // khách vừa bấm thử lại: liên kết thanh toán mới còn hiệu lực
        }

        stockRelease.release(order);
        order.setStatus(OrderStatus.CANCELLED);
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
        }
        notificationService.notify(order.getAccountId(), NotificationType.ORDER_STATUS_CHANGED,
                "Đơn hàng #" + order.getShortCode() + " đã tự động hủy vì chưa thanh toán VNPay trong " + holdMinutes
                        + " phút. Bạn có thể đặt lại nếu sản phẩm còn hàng.");
        return true;
    }
}
