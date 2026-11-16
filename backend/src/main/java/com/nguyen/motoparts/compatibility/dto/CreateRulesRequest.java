package com.nguyen.motoparts.compatibility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Admin khai báo: phụ tùng này lắp được cho dòng xe kia trong khoảng năm [yearFrom, yearTo]. */
public record CreateRulesRequest(
        @NotBlank(message = "Vui lòng chọn phụ tùng.")
        String sparePartId,

        @NotBlank(message = "Vui lòng chọn hãng xe.")
        String brand,

        @NotBlank(message = "Vui lòng chọn dòng xe.")
        String model,

        @NotNull(message = "Vui lòng nhập năm bắt đầu.")
        Integer yearFrom,

        @NotNull(message = "Vui lòng nhập năm kết thúc.")
        Integer yearTo) {
}