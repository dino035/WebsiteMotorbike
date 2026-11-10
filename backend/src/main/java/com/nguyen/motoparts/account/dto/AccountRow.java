package com.nguyen.motoparts.account.dto;

import java.time.LocalDateTime;

/** Một dòng trong danh sách tài khoản của admin. */
public record AccountRow(
        String accountId,
        String email,
        String phone,
        boolean locked,
        String lockReason,
        LocalDateTime lockedAt,
        /** Số đơn chưa hoàn tất (chờ xác nhận, đã xác nhận, đang giao). */
        long unfinishedOrders) {
}
