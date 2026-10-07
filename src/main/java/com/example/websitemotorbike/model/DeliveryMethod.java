package com.example.websitemotorbike.model;

import lombok.Getter;

/** Hình thức nhận hàng: giao tận nơi hoặc đến cửa hàng nhận (SRS 2.3.2). */
@Getter
public enum DeliveryMethod {
    DELIVERY("Giao hàng tận nơi"),
    PICKUP("Nhận tại cửa hàng");

    private final String label;

    DeliveryMethod(String label) {
        this.label = label;
    }
}
