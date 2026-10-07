package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một dòng trong giỏ hàng. Chỉ giữ productId kiểu String, KHÔNG đặt @ManyToOne Product
 * (quy ước README mục 3); thông tin sản phẩm lấy bằng ProductDB.selectById.
 * addedAt dùng để tính thời hạn tạm giữ đối với xe (sản phẩm độc bản).
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String cartItemId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cartId")
    private ShoppingCart cart;

    private String productId;

    private int quantity;

    private LocalDateTime addedAt;
}
