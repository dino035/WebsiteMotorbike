package com.nguyen.motoparts.compatibility.entity;

/**
 * Thuộc giai đoạn Khóa luận (tìm kiếm ngữ nghĩa), CHƯA dùng ở Tiểu luận.
 * Tạm bỏ @Entity để Hibernate không đòi @Id khi khởi động; repository tương ứng cũng tạm xóa.
 * Đặc tả: embeddingId, vector: String, modelVersion: String; Composition 1 — 0..1 từ Product.
 */
public class ProductEmbedding {
}
