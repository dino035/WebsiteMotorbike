package com.nguyen.motoparts.advisory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Thêm hoặc sửa hồ sơ xe: biệt danh (không bắt buộc), dòng xe lấy từ danh sách dòng xe, năm đăng ký. */
public record VehicleProfileRequest(
        @Size(max = 50, message = "Biệt danh tối đa 50 ký tự.")
        String nickname,

        @NotBlank(message = "Vui lòng chọn dòng xe.")
        String vehicleModelId,

        @NotNull(message = "Vui lòng nhập năm đăng ký.")
        Integer registrationYear) {
}
