package com.example.websitemotorbike.model;

import lombok.Getter;

/**
 * Hình thức nhận hàng (SRS 2.3.2): giao tận nơi, nhận tại cửa hàng, hoặc lắp đặt tại cửa hàng
 * (chỉ khi đơn có phụ tùng, có tính thêm phí công). Không đổi tên hằng số vì CSDL lưu chuỗi này.
 */
@Getter
public enum DeliveryMethod {
    DELIVERY("Giao hàng tận nơi"),
    PICKUP("Nhận tại cửa hàng"),
    INSTALL("Lắp đặt tại cửa hàng");

    private final String label;

    DeliveryMethod(String label) {
        this.label = label;
    }
}
