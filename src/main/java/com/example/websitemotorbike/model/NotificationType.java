package com.example.websitemotorbike.model;

import lombok.Getter;

// [CẦN CHỐT] Danh sách loại thông báo đang tạm đặt, cần xác nhận với Nhóm 1
// (ví dụ: có cần loại thông báo thanh toán không).
@Getter
public enum NotificationType {
    ORDER_PLACED("Đặt hàng thành công"),
    ORDER_STATUS_CHANGED("Cập nhật đơn hàng"),
    WARRANTY_UPDATED("Cập nhật bảo hành"),
    SYSTEM("Thông báo hệ thống");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }
}