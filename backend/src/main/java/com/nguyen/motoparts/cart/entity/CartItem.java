package com.nguyen.motoparts.cart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một dòng trong giỏ. Giữ productId kiểu String (đặc tả), không @ManyToOne Product.
 * addedAt giữ từ WebsiteMotorbike: dùng tính thời hạn tạm giữ xe (sản phẩm độc bản).
 */
@Entity
@Getter
@Setter
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String cartItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private ShoppingCart cart;

    @Column(nullable = false)
    private String productId;

    private int quantity;

    private LocalDateTime addedAt;
}
