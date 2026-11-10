package com.nguyen.motoparts.aftersales.entity;

import com.nguyen.motoparts.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Thông báo cho một tài khoản. Giữ accountId kiểu String, không nối User (đặc tả). */
@Entity
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String notificationId;

    @Column(nullable = false)
    private String accountId;

    @Column(length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    /** Kiểu Boolean (không phải boolean) để getter là getIsRead() và JSON ra đúng tên isRead. */
    @Column(nullable = false)
    private Boolean isRead = false;

    /** Hệ thống tự đặt khi lưu, để sắp xếp thông báo mới nhất lên đầu. */
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
