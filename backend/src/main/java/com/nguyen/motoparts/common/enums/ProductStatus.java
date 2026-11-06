package com.nguyen.motoparts.common.enums;

import lombok.Getter;

/** Trạng thái sản phẩm (đặc tả: Có sẵn / Hết hàng). Giữ nguyên tên hằng số như WebsiteMotorbike. */
@Getter
public enum ProductStatus {
    AVAILABLE("Có sẵn"),
    SOLD("Hết hàng");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }
}
