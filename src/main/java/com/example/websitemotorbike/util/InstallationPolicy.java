package com.example.websitemotorbike.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Quy tắc lắp đặt tại cửa hàng (SRS 3.11). Các con số dưới đây là GIẢ ĐỊNH, SRS chưa nêu,
 * cần chốt với nhóm: phí công cố định mỗi đơn, số lượt tối đa mỗi ngày, và khoảng ngày được đặt.
 */
public class InstallationPolicy {

    /** Phí công lắp đặt cộng vào tổng đơn (một lần cho mỗi đơn). */
    public static final BigDecimal LABOR_FEE = BigDecimal.valueOf(50000);

    /** Số lượt lắp đặt tối đa mỗi ngày (không tính lịch đã hủy). */
    public static final int MAX_PER_DAY = 5;

    /** Chỉ nhận lịch trong vòng bấy nhiêu ngày kể từ hôm nay. */
    public static final int MAX_DAYS_AHEAD = 60;

    /** Chuyển chuỗi yyyy-MM-dd (từ input type=date) thành ngày. */
    public static LocalDate parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ngày hẹn lắp đặt");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày hẹn không hợp lệ");
        }
    }

    /** Ngày hẹn phải từ ngày mai trở đi và trong khoảng cho phép. */
    public static void validateDate(LocalDate date, LocalDate today) {
        if (!date.isAfter(today)) {
            throw new IllegalArgumentException("Ngày hẹn phải từ ngày mai trở đi");
        }
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException("Chỉ nhận lịch trong vòng " + MAX_DAYS_AHEAD + " ngày tới");
        }
    }
}
