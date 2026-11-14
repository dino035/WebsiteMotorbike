package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.common.exception.BadRequestException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Quy tắc lắp đặt phụ tùng tại cửa hàng (SRS 3.11). Các con số dưới đây là GIẢ ĐỊNH chuyển từ bản JSP
 * (SRS chưa nêu), cần chốt với nhóm: phí công cố định mỗi đơn, số lượt tối đa mỗi ngày, khoảng ngày được đặt.
 */
public final class InstallationPolicy {

    /** Phí công lắp đặt, cộng vào tổng đơn một lần cho mỗi đơn. */
    public static final BigDecimal LABOR_FEE = BigDecimal.valueOf(50000);

    /** Số lượt lắp đặt tối đa mỗi ngày (không tính lịch đã hủy). */
    public static final int MAX_PER_DAY = 5;

    /** Chỉ nhận lịch trong vòng bấy nhiêu ngày kể từ hôm nay. */
    public static final int MAX_DAYS_AHEAD = 60;

    private InstallationPolicy() {
    }

    /** Ngày hẹn phải từ ngày mai trở đi và nằm trong khoảng cho phép. */
    public static void validateDate(LocalDate date, LocalDate today) {
        if (date == null) {
            throw new BadRequestException("Vui lòng chọn ngày hẹn lắp đặt.");
        }
        if (!date.isAfter(today)) {
            throw new BadRequestException("Ngày hẹn phải từ ngày mai trở đi.");
        }
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new BadRequestException("Chỉ nhận lịch trong vòng " + MAX_DAYS_AHEAD + " ngày tới.");
        }
    }
}
