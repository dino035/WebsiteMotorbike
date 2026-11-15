package com.nguyen.motoparts.aftersales.service;

import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.aftersales.dto.AdminWarrantyDetail;
import com.nguyen.motoparts.aftersales.dto.AdminWarrantyRow;
import com.nguyen.motoparts.aftersales.entity.WarrantyRequest;
import com.nguyen.motoparts.aftersales.repository.WarrantyRequestRepository;
import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.common.enums.NotificationType;
import com.nguyen.motoparts.common.enums.WarrantyStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.order.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin xử lý yêu cầu bảo hành (SRS 3.22): xem danh sách, chi tiết; duyệt (thay mới) hoặc từ chối một yêu cầu đang chờ.
 * Từ chối bắt buộc có lý do. Khóa dòng yêu cầu (SELECT ... FOR UPDATE) nên hai Admin không quyết định cùng lúc.
 * Sau khi lưu có thông báo quyết định cho khách.
 */
@Service
public class AdminWarrantyService {

    private final WarrantyRequestRepository repository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WarrantyRequestService customerService;

    public AdminWarrantyService(WarrantyRequestRepository repository, UserRepository userRepository,
                                NotificationService notificationService, WarrantyRequestService customerService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.customerService = customerService;
    }

    /** Mọi yêu cầu, mới nhất trước; statusText trống hoặc ALL thì không lọc. */
    @Transactional(readOnly = true)
    public PageResponse<AdminWarrantyRow> list(String statusText, int page, int size) {
        WarrantyStatus status = parseStatus(statusText);
        PageRequest pageable = PageRequest.of(page, size);
        Page<WarrantyRequest> requests = status == null
                ? repository.findAllByOrderBySubmittedAtDesc(pageable)
                : repository.findByStatusOrderBySubmittedAtDesc(status, pageable);

        Map<String, String> emails = userRepository
                .findAllById(requests.stream().map(WarrantyRequest::getAccountId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getAccountId, User::getEmail, (a, b) -> a));
        return PageResponse.from(requests.map(w -> new AdminWarrantyRow(w.getRequestId(), w.getShortCode(),
                w.getSubmittedAt(), w.getStatus().name(), w.getStatus().getLabel(),
                w.getOrderItem().getProductName(), w.getOrderItem().getOrder().getShortCode(),
                emails.get(w.getAccountId()))));
    }

    @Transactional(readOnly = true)
    public AdminWarrantyDetail detail(String requestId) {
        WarrantyRequest w = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu bảo hành."));
        return toDetail(w);
    }

    /** Duyệt (thay mới) hoặc từ chối (cần lý do) một yêu cầu đang chờ xử lý. */
    @Transactional
    public AdminWarrantyDetail decide(String requestId, boolean approve, String note) {
        String cleanNote = note == null ? "" : note.trim();
        if (!approve && cleanNote.isEmpty()) {
            throw new BadRequestException("Vui lòng nhập lý do từ chối.");
        }
        WarrantyRequest w = repository.findForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu bảo hành."));
        if (w.getStatus() != WarrantyStatus.PENDING) {
            throw new ConflictException("Yêu cầu này đã được xử lý.");
        }
        w.setStatus(approve ? WarrantyStatus.APPROVED : WarrantyStatus.REJECTED);
        w.setDecisionReason(cleanNote.isEmpty() ? null : cleanNote);
        w.setResolvedAt(LocalDateTime.now());

        String content = approve
                ? "Yêu cầu bảo hành #" + w.getShortCode() + " đã được duyệt, sản phẩm sẽ được thay mới"
                : "Yêu cầu bảo hành #" + w.getShortCode() + " bị từ chối. Lý do: " + cleanNote;
        notificationService.notify(w.getAccountId(), NotificationType.WARRANTY_UPDATED, content);
        return toDetail(w);
    }

    private AdminWarrantyDetail toDetail(WarrantyRequest w) {
        OrderItem item = w.getOrderItem();
        LocalDateTime purchasedAt = item.getOrder().getOrderDate();
        User user = userRepository.findById(w.getAccountId()).orElse(null);
        return new AdminWarrantyDetail(
                WarrantyRequestService.toResponse(w),
                new AdminWarrantyDetail.Customer(w.getAccountId(), user == null ? null : user.getEmail(),
                        user == null ? null : user.getPhone()),
                purchasedAt, WarrantyPolicy.deadline(purchasedAt, customerService.isMotorbike(item)),
                w.getStatus() == WarrantyStatus.PENDING);
    }

    private static WarrantyStatus parseStatus(String text) {
        if (text == null || text.isBlank() || "ALL".equalsIgnoreCase(text.trim())) {
            return null;
        }
        try {
            return WarrantyStatus.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái không hợp lệ.");
        }
    }
}
