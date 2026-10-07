package com.example.websitemotorbike.model;

import lombok.Getter;

/** Trạng thái xử lý đơn hàng (SRS 3.12). Không đổi tên hằng số vì CSDL lưu chuỗi này. */
@Getter
public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao/Chờ lấy"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }
}
