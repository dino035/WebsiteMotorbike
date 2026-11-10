package com.nguyen.motoparts.account.dto;

import jakarta.validation.constraints.Size;

/** force = true khi admin đã xem cảnh báo "còn đơn chưa hoàn tất" và vẫn muốn khóa. */
public record LockRequest(
        @Size(max = 200, message = "Lý do khóa quá dài (tối đa 200 ký tự).")
        String reason,
        boolean force) {
}
