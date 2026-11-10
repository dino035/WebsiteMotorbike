package com.nguyen.motoparts.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** quantity bỏ trống nghĩa là 1. Với xe máy luôn là 1 dù gửi bao nhiêu. */
public record AddToCartRequest(
        @NotBlank(message = "Thiếu mã sản phẩm.")
        String productId,

        @Min(value = 1, message = "Số lượng phải từ 1 đến 99.")
        @Max(value = 99, message = "Số lượng phải từ 1 đến 99.")
        Integer quantity) {
}
