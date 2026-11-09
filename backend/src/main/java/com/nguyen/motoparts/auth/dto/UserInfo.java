package com.nguyen.motoparts.auth.dto;

import com.nguyen.motoparts.common.enums.UserRole;

/** Thông tin tài khoản trả về cho React (không bao giờ có mật khẩu). */
public record UserInfo(String accountId, String email, String phone, UserRole role) {
}
