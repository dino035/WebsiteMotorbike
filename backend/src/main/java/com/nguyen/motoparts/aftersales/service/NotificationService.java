package com.nguyen.motoparts.aftersales.service;

import com.nguyen.motoparts.aftersales.entity.Notification;
import com.nguyen.motoparts.aftersales.repository.NotificationRepository;
import com.nguyen.motoparts.common.enums.NotificationType;
import org.springframework.stereotype.Service;

/**
 * Thông báo cho khách. Hiện mới có phần TẠO thông báo (đơn hàng gọi vào đây); xem danh sách, đánh dấu đã đọc
 * sẽ làm cùng trang thông báo.
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
}
