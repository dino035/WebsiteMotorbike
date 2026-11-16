package com.nguyen.motoparts.aftersales.dto;

import com.nguyen.motoparts.aftersales.entity.Notification;

import java.time.LocalDateTime;

/** Một thông báo của khách: loại (kèm nhãn tiếng Việt), nội dung, đã đọc chưa, thời điểm tạo. */
public record NotificationResponse(String notificationId, String type, String typeLabel, String content,
                                   boolean isRead, LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getNotificationId(),
                n.getType() == null ? null : n.getType().name(),
                n.getType() == null ? "" : n.getType().getLabel(),
                n.getContent(),
                Boolean.TRUE.equals(n.getIsRead()),
                n.getCreatedAt());
    }
}
