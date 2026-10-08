package com.example.websitemotorbike.util;

import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.Product;

import java.time.LocalDateTime;

/**
 * Chính sách bảo hành (SRS 2.3.2): xe 2 năm, phụ tùng 6 tháng, tính từ ngày mua.
 * Sản phẩm không còn trong hệ thống (product == null) được xử lý như phụ tùng (thời hạn ngắn hơn).
 */
public class WarrantyPolicy {

    public static final int MOTORBIKE_MONTHS = 24;
    public static final int SPARE_PART_MONTHS = 6;

    public static int monthsFor(Product product) {
        return product instanceof Motorbike ? MOTORBIKE_MONTHS : SPARE_PART_MONTHS;
    }

    public static LocalDateTime deadline(LocalDateTime purchasedAt, Product product) {
        return purchasedAt.plusMonths(monthsFor(product));
    }

    public static boolean isWithin(LocalDateTime purchasedAt, Product product, LocalDateTime now) {
        return !now.isAfter(deadline(purchasedAt, product));
    }
}
