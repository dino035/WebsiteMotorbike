package com.nguyen.motoparts.common.enums;

import lombok.Getter;

/** Hình thức nhận hàng (SRS 2.3.2). INSTALL chỉ dùng khi đơn có phụ tùng, có tính thêm phí công. */
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
