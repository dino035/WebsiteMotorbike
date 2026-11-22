package com.nguyen.motoparts.aftersales.dto;

import com.nguyen.motoparts.aftersales.entity.ProductReview;

import java.time.LocalDateTime;

/** Một đánh giá hiển thị công khai ở trang chi tiết sản phẩm. */
public record ReviewResponse(
        String reviewId,
        String reviewer,
        int rating,
        String comment,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(ProductReview review, String email) {
        return new ReviewResponse(
                review.getReviewId(),
                maskEmail(email),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }

    /**
     * Che bớt email trước khi đưa ra trang công khai: "nguyenvana@gmail.com" thành "ngu***@gmail.com".
     * Trang chi tiết sản phẩm ai cũng xem được, hiện nguyên email là lộ danh sách khách đã mua.
     */
    private static String maskEmail(String email) {
        if (email == null) {
            return "Khách hàng";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "Khách hàng";
        }
        String name = email.substring(0, at);
        String visible = name.length() <= 3 ? name.substring(0, 1) : name.substring(0, 3);
        return visible + "***" + email.substring(at);
    }
}