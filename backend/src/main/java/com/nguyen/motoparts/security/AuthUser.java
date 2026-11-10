package com.nguyen.motoparts.security;

import com.nguyen.motoparts.common.enums.UserRole;

/** Người dùng đã xác thực của request hiện tại (principal trong SecurityContext). */
public record AuthUser(String accountId, String email, UserRole role) {
}
