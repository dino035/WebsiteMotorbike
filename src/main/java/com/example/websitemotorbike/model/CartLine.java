package com.example.websitemotorbike.model;

import java.math.BigDecimal;

/**
 * Một dòng hiển thị trên trang giỏ hàng: ghép CartItem (số lượng) với Product (tên, giá).
 * Không phải entity, chỉ dùng cho view.
 */
public class CartLine {

    private final String cartItemId;
    private final String productId;
    private final String name;
    private final String productType;
    private final boolean motorbike;
    private final BigDecimal unitPrice;
    private final int quantity;

    public CartLine(CartItem item, Product product) {
        this.cartItemId = item.getCartItemId();
        this.productId = item.getProductId();
        this.name = product.getName();
        this.productType = product.getProductType();
        this.motorbike = product instanceof Motorbike;
        this.unitPrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        this.quantity = item.getQuantity();
    }

    public String getCartItemId() {
        return cartItemId;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getProductType() {
        return productType;
    }

    public boolean isMotorbike() {
        return motorbike;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
