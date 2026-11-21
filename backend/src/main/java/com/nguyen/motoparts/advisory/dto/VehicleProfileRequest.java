package com.nguyen.motoparts.advisory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Thêm hoặc sửa hồ sơ xe (SRS 3.15): biệt danh bắt buộc (tối đa 50 ký tự), dòng xe lấy từ danh sách dòng xe, năm đăng ký. */
public record VehicleProfileRequest(
        @NotBlank(message = "Vui lòng nhập biệt danh cho xe.")
        @Size(max = 50, message = "Biệt danh tối đa 50 ký tự.")
        String nickname,

        @NotBlank(message = "Vui lòng chọn dòng xe.")
        String vehicleModelId,

        @NotNull(message = "Vui lòng nhập năm đăng ký.")
        Integer registrationYear) {
}
