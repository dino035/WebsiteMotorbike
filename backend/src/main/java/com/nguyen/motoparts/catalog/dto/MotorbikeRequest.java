package com.nguyen.motoparts.catalog.dto;

import com.nguyen.motoparts.common.enums.ProductStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MotorbikeRequest(
        @NotBlank(message = "Vui lòng nhập tên xe.")
        String name,

        @NotNull(message = "Vui lòng nhập giá.")
        @DecimalMin(value = "0", inclusive = false, message = "Giá phải lớn hơn 0.")
        BigDecimal price,

        @NotNull(message = "Vui lòng chọn tình trạng.")
        ProductStatus status,

        @NotBlank(message = "Vui lòng nhập hãng xe.")
        String brand,

        @NotBlank(message = "Vui lòng nhập dòng xe.")
        String model,

        @NotNull(message = "Vui lòng nhập năm đăng ký.")
        @Min(value = 1980, message = "Năm đăng ký không hợp lệ.")
        @Max(value = 2100, message = "Năm đăng ký không hợp lệ.")
        Integer registrationYear,

        @NotNull(message = "Vui lòng nhập số đời chủ.")
        @Min(value = 0, message = "Số đời chủ không được âm.")
        Integer ownerCount,

        @NotBlank(message = "Vui lòng nhập lịch sử tai nạn. Gõ 'Không' nếu xe chưa từng tai nạn.")
        String accidentHistory,

        @NotBlank(message = "Vui lòng nhập số khung.")
        String chassisNumber,

        @NotBlank(message = "Vui lòng nhập số máy.")
        String engineNumber,

        @NotNull(message = "Vui lòng nhập số km đã đi.")
        @Min(value = 0, message = "Số km không được âm.")
        Integer odometerAtIntake) {
}