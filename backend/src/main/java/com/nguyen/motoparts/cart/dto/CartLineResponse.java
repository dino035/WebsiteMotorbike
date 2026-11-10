package com.nguyen.motoparts.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một dòng trên trang giỏ hàng: ghép CartItem (số lượng) với Product (tên, giá). */
public record CartLineResponse(
        String cartItemId,
        String productId,
        String name,
        String typeCode,
        String productType,
        boolean motorbike,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        String thumbnailUrl,
        /** Chỉ có với xe máy: thời điểm hết hạn tạm giữ. Quá hạn mà khách khác thêm xe này thì dòng bị gỡ. */
        LocalDateTime holdExpiresAt) {
}
