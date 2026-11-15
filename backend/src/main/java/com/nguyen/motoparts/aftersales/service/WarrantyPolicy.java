package com.nguyen.motoparts.aftersales.service;

import java.time.LocalDateTime;

/**
 * Chính sách bảo hành (SRS 2.3.2): xe 2 năm, phụ tùng 6 tháng, tính từ ngày mua (ngày đặt đơn).
 * Sản phẩm không còn trong hệ thống được xử lý như phụ tùng (thời hạn ngắn hơn).
 */
public final class WarrantyPolicy {

    public static final int MOTORBIKE_MONTHS = 24;
    public static final int SPARE_PART_MONTHS = 6;

    private WarrantyPolicy() {
    }

    public static int monthsFor(boolean motorbike) {
        return motorbike ? MOTORBIKE_MONTHS : SPARE_PART_MONTHS;
    }

    public static LocalDateTime deadline(LocalDateTime purchasedAt, boolean motorbike) {
        return purchasedAt.plusMonths(monthsFor(motorbike));
    }

    public static boolean isWithin(LocalDateTime purchasedAt, boolean motorbike, LocalDateTime now) {
        return !now.isAfter(deadline(purchasedAt, motorbike));
    }
}
