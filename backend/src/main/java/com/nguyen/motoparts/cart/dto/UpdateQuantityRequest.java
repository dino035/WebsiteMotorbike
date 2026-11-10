package com.nguyen.motoparts.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateQuantityRequest(
        @NotNull(message = "Vui lòng nhập số lượng.")
        @Min(value = 1, message = "Số lượng phải từ 1 đến 99.")
        @Max(value = 99, message = "Số lượng phải từ 1 đến 99.")
        Integer quantity) {
}
