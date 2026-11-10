package com.nguyen.motoparts.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Giỏ hàng để hiển thị. notices là các dòng vừa bị gỡ vì không còn khả dụng (hết hàng, xe đã có người khác giữ);
 * React hiện chúng một lần cho khách.
 */
public record CartResponse(
        List<CartLineResponse> lines,
        BigDecimal total,
        int itemCount,
        List<String> notices,
        int holdMinutes) {
}
