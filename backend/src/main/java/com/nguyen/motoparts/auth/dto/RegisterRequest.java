package com.nguyen.motoparts.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Vui lòng nhập email.")
        @Email(message = "Email không hợp lệ.")
        String email,

        @NotBlank(message = "Vui lòng nhập mật khẩu.")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự.")
        String password,

        @Pattern(regexp = "^$|^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.")
        String phone) {
}
