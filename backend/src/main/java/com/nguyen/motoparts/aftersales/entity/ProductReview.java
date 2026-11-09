package com.nguyen.motoparts.aftersales.entity;

import com.nguyen.motoparts.catalog.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Đánh giá sản phẩm (Verified Purchase). Theo đặc tả: Association 0..* — 1 tới Product (không được bỏ);
 * WebsiteMotorbike chỉ giữ productId kiểu String, ở đây đổi thành @ManyToOne Product cho đúng đặc tả.
 * Mỗi khách chỉ đánh giá một sản phẩm một lần (ràng buộc unique, sửa hạn chế của bản JSP).
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "product_id"}))
@Getter
@Setter
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String reviewId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    /** 1 đến 5 sao (kiểm tra ở DTO). */
    private int rating;

    @Column(length = 1000)
    private String comment;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
