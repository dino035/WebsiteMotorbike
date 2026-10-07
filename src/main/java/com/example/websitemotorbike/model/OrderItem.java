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

import java.math.BigDecimal;

/**
 * Một dòng trong đơn hàng. Chỉ giữ productId kiểu String (không @ManyToOne Product, quy ước README).
 * productName và unitPrice là bản chụp tại thời điểm đặt, để đơn không đổi khi sản phẩm bị sửa hay xóa.
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String orderItemId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orderId")
    private Order order;

    private String productId;

    private String productName;

    private BigDecimal unitPrice;

    private int quantity;

    public BigDecimal getLineTotal() {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
