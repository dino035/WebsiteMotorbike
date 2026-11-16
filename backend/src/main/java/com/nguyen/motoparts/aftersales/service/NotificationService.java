package com.nguyen.motoparts.aftersales.service;

import com.nguyen.motoparts.aftersales.dto.NotificationResponse;
import com.nguyen.motoparts.aftersales.entity.Notification;
import com.nguyen.motoparts.aftersales.repository.NotificationRepository;
import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.common.enums.NotificationType;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thông báo cho khách: tạo (đơn hàng, bảo hành gọi vào đây), xem danh sách, đếm chưa đọc, đánh dấu đã đọc.
 * Mọi thao tác xem/sửa đều giới hạn theo accountId lấy từ token, không nhận accountId từ client.
 */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /** Lưu một thông báo chưa đọc. Chạy trong giao dịch của người gọi (đặt hàng hỏng thì thông báo cũng không lưu). */
    public Notification notify(String accountId, NotificationType type, String content) {
        Notification n = new Notification();
        n.setAccountId(accountId);
        n.setType(type);
        n.setContent(content);
        return repository.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listMine(String accountId, int page, int size) {
        return PageResponse.from(repository.findMine(accountId, PageRequest.of(page, size)).map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(String accountId) {
        return repository.countUnread(accountId);
    }

    /** Đánh dấu đã đọc một thông báo của mình; gọi lại nhiều lần vẫn được. Của người khác coi như không tồn tại. */
    @Transactional
    public NotificationResponse markRead(String accountId, String notificationId) {
        if (repository.markRead(notificationId, accountId) == 0) {
            throw new ResourceNotFoundException("Không tìm thấy thông báo này.");
        }
        return NotificationResponse.from(repository.findById(notificationId).orElseThrow());
    }

    @Transactional
    public int markAllRead(String accountId) {
        return repository.markAllRead(accountId);
    }
}
