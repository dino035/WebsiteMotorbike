package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Giỏ hàng của một tài khoản (mỗi tài khoản một giỏ).
 * Chỉ giữ accountId kiểu String, không đặt @ManyToOne User (quy ước README mục 3).
 * Không dùng cache chung của EclipseLink vì giỏ hàng bị nhiều phiên cùng thay đổi.
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String cartId;

    private String accountId;

    private LocalDateTime createdAt;
}
