package com.nguyen.motoparts.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Thêm hoặc sửa một địa chỉ (đặc tả: houseNumber, street, district, city; thêm người nhận từ bản JSP). */
public record AddressRequest(
        @NotBlank(message = "Vui lòng nhập họ tên người nhận.")
        @Size(max = 100, message = "Họ tên người nhận quá dài (tối đa 100 ký tự).")
        String receiverName,

        @NotBlank(message = "Vui lòng nhập số điện thoại.")
        String receiverPhone,

        @NotBlank(message = "Vui lòng nhập số nhà.")
        @Size(max = 100, message = "Số nhà quá dài (tối đa 100 ký tự).")
        String houseNumber,

        @NotBlank(message = "Vui lòng nhập tên đường.")
        @Size(max = 150, message = "Tên đường quá dài (tối đa 150 ký tự).")
        String street,

        @NotBlank(message = "Vui lòng nhập quận/huyện.")
        @Size(max = 100, message = "Quận/huyện quá dài (tối đa 100 ký tự).")
        String district,

        @NotBlank(message = "Vui lòng nhập tỉnh/thành phố.")
        @Size(max = 100, message = "Tỉnh/thành phố quá dài (tối đa 100 ký tự).")
        String city,

        /** Chọn làm địa chỉ mặc định (địa chỉ đầu tiên của tài khoản luôn là mặc định). */
        boolean makeDefault) {
}
