package com.nguyen.motoparts.catalog.dto;

import com.nguyen.motoparts.common.enums.PartOriginType;
import com.nguyen.motoparts.common.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SparePartRequest(
        @NotBlank(message = "Vui lòng nhập tên phụ tùng.")
        String name,

        @NotNull(message = "Vui lòng nhập giá.")
        @DecimalMin(value = "0", inclusive = false, message = "Giá phải lớn hơn 0.")
        BigDecimal price,

        @NotNull(message = "Vui lòng chọn tình trạng.")
        ProductStatus status,

        @NotBlank(message = "Vui lòng nhập loại phụ tùng.")
        String partType,

        @NotNull(message = "Vui lòng chọn nguồn gốc.")
        PartOriginType partOrigin) {
}