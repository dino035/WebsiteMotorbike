package com.nguyen.motoparts.common.enums;

import lombok.Getter;

/** Loại thông báo, giữ như WebsiteMotorbike. */
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
