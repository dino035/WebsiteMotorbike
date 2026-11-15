package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.aftersales.service.NotificationService;
import com.nguyen.motoparts.common.enums.InstallationStatus;
import com.nguyen.motoparts.common.enums.NotificationType;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.order.dto.InstallationActionRequest;
import com.nguyen.motoparts.order.dto.InstallationRow;
import com.nguyen.motoparts.order.entity.InstallationRequest;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.repository.InstallationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
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

    public AdminInstallationService(InstallationRequestRepository repository, UserRepository userRepository,
                                    NotificationService notificationService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
                request.setStatus(InstallationStatus.CANCELLED);
                message = "Lịch lắp đặt của đơn hàng #" + code + " đã bị hủy";
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
