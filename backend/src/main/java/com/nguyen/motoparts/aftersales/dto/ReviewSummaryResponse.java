package com.nguyen.motoparts.aftersales.dto;

import java.util.List;

/** Toàn bộ dữ liệu đánh giá của một sản phẩm, trả trong một lần gọi. */
public record ReviewSummaryResponse(
        double average,
        int count,
        List<ReviewResponse> items
) {
}