package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.aftersales.service.NotificationService;
import com.nguyen.motoparts.common.enums.InstallationStatus;
import com.nguyen.motoparts.common.enums.NotificationType;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.enums.PaymentStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.order.dto.InstallationActionRequest;
import com.nguyen.motoparts.order.dto.InstallationRow;
import com.nguyen.motoparts.order.entity.InstallationRequest;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.entity.Payment;
import com.nguyen.motoparts.order.repository.InstallationRequestRepository;
import com.nguyen.motoparts.order.repository.OnlinePaymentRepository;
import com.nguyen.motoparts.order.repository.OrderRepository;
import com.nguyen.motoparts.order.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin quản lý lịch lắp đặt (SRS 3.21). Chỉ xử lý lịch đang SCHEDULED: đánh dấu đã lắp xong, hủy lịch hoặc đổi ngày
 * (kiểm tra ngày còn chỗ, khóa theo ngày như lúc đặt hàng). Sau khi lưu có thông báo cho khách.
 */
@Service
public class AdminInstallationService {

    private final InstallationRequestRepository repository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OnlinePaymentRepository onlinePaymentRepository;
    private final VnpayGateway vnpayGateway;

    public AdminInstallationService(InstallationRequestRepository repository, UserRepository userRepository,
                                    NotificationService notificationService, OrderRepository orderRepository,
                                    PaymentRepository paymentRepository, OnlinePaymentRepository onlinePaymentRepository,
                                    VnpayGateway vnpayGateway) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.onlinePaymentRepository = onlinePaymentRepository;
        this.vnpayGateway = vnpayGateway;
    }

    /** Mọi lịch, ngày hẹn gần nhất trước; statusText trống thì không lọc. */
    @Transactional(readOnly = true)
    public List<InstallationRow> list(String statusText) {
        InstallationStatus status = parseStatus(statusText);
        List<InstallationRequest> requests = status == null
                ? repository.findAllByOrderByPreferredDateAscCreatedAtAsc()
                : repository.findByStatusOrderByPreferredDateAscCreatedAtAsc(status);
        Map<String, String> emails = userRepository
                .findAllById(requests.stream().map(InstallationRequest::getAccountId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getAccountId, User::getEmail, (a, b) -> a, java.util.HashMap::new));
        return requests.stream().map(r -> toRow(r, emails.get(r.getAccountId()))).toList();
    }

    @Transactional
    public InstallationRow act(String requestId, InstallationActionRequest req) {
        // Khóa đơn trước, lịch sau: cùng thứ tự với Admin đổi trạng thái đơn và với kết quả VNPay (cũng khóa đơn trước),
        // nên không giẫm lên nhau và không chờ nhau vòng tròn. Mã đơn lấy riêng để không nạp lịch vào bộ nhớ đệm trước khóa.
        String orderId = repository.findOrderIdByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch lắp đặt."));
        orderRepository.findForUpdate(orderId);
        InstallationRequest request = repository.findForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch lắp đặt."));
        if (request.getStatus() != InstallationStatus.SCHEDULED) {
            throw new ConflictException("Lịch lắp đặt này đã được xử lý.");
        }
        Order order = request.getOrder();
        String code = order.getShortCode();
        String message;

        switch (req.action()) {
            case DONE -> {
                if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CANCELLED) {
                    throw new ConflictException("Hãy xác nhận đơn hàng trước khi đánh dấu đã lắp đặt.");
                }
                request.setStatus(InstallationStatus.DONE);
                message = "Đã lắp đặt xong phụ tùng cho đơn hàng #" + code;
            }
            case CANCEL -> {
                Payment payment = paymentRepository.findByOrderId(order.getOrderId()).orElse(null);
                BigDecimal fee = request.getLaborFee() == null ? BigDecimal.ZERO : request.getLaborFee();
                if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
                    // Đã thu tiền thì không trừ: số đã thu và số trên đơn sẽ lệch nhau, mà hoàn tiền chưa có luồng riêng.
                    throw new ConflictException("Đơn hàng đã thu tiền nên chưa thể hủy riêng lịch lắp đặt. Hệ thống chưa có "
                            + "luồng hoàn tiền: nếu khách không cần lắp nữa, hãy liên hệ khách và hoàn lại phí công "
                            + vnd(fee) + " bằng tay.");
                }
                if (hasLiveOnlineLink(order.getOrderId())) {
                    throw new ConflictException("Khách đang có liên kết thanh toán VNPay còn hiệu lực với số tiền cũ. "
                            + "Hãy thử lại sau vài phút, khi liên kết đó hết hạn.");
                }
                request.setStatus(InstallationStatus.CANCELLED);
                order.recalculateTotal();                 // bỏ phí công khỏi tổng đơn
                if (payment != null) {
                    payment.setAmount(order.getTotalAmount());   // số tiền cần thu (COD hoặc VNPay lần sau) theo tổng mới
                }
                message = "Lịch lắp đặt của đơn hàng #" + code + " đã bị hủy, phí công " + vnd(fee)
                        + " được trừ khỏi đơn. Tổng tiền mới: " + vnd(order.getTotalAmount());
            }
            case RESCHEDULE -> {
                LocalDate newDate = req.newDate();
                InstallationPolicy.validateDate(newDate, LocalDate.now());
                if (newDate.equals(request.getPreferredDate())) {
                    throw new BadRequestException("Lịch hẹn vẫn là ngày này, hãy chọn ngày khác.");
                }
                repository.lockDay(newDate.toEpochDay());   // hai thao tác cùng ngày chạy lần lượt
                if (repository.countByPreferredDateAndStatusNot(newDate, InstallationStatus.CANCELLED)
                        >= InstallationPolicy.MAX_PER_DAY) {
                    throw new ConflictException("Ngày này đã kín lịch, vui lòng chọn ngày khác.");
                }
                request.setPreferredDate(newDate);
                message = "Lịch lắp đặt của đơn hàng #" + code + " đã được đổi sang ngày " + newDate;
            }
            default -> throw new BadRequestException("Thao tác không hợp lệ.");
        }
        notificationService.notify(request.getAccountId(), NotificationType.ORDER_STATUS_CHANGED, message);
        String email = userRepository.findById(request.getAccountId()).map(User::getEmail).orElse(null);
        return toRow(request, email);
    }

    /** Khách còn một liên kết VNPay chưa có kết quả (kể cả thời gian chờ kết quả về): số tiền trong đó là số cũ. */
    private boolean hasLiveOnlineLink(String orderId) {
        LocalDateTime now = LocalDateTime.now();
        int minutes = vnpayGateway.expireMinutes() + VnpayOrderExpiryService.GRACE_MINUTES;
        return onlinePaymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .anyMatch(a -> a.getStatus() == PaymentStatus.PENDING && a.getCreatedAt() != null
                        && a.getCreatedAt().plusMinutes(minutes).isAfter(now));
    }

    private static String vnd(BigDecimal amount) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + "đ";
    }

    private static InstallationRow toRow(InstallationRequest r, String email) {
        Order o = r.getOrder();
        return new InstallationRow(r.getRequestId(), o.getOrderId(), o.getShortCode(),
                o.getStatus().name(), o.getStatus().getLabel(), email, o.getReceiverName(), o.getReceiverPhone(),
                r.getPreferredDate(), r.getLaborFee(), r.getStatus().name(), r.getStatus().getLabel());
    }

    private static InstallationStatus parseStatus(String text) {
        if (text == null || text.isBlank() || "ALL".equalsIgnoreCase(text.trim())) {
            return null;
        }
        try {
            return InstallationStatus.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái không hợp lệ.");
        }
    }
}
