package com.nguyen.motoparts.order.dto;

import java.math.BigDecimal;

/** Các quy tắc React cần để vẽ trang đặt hàng (phí công, giới hạn ngày hẹn lắp đặt). */
public record CheckoutPolicyResponse(BigDecimal laborFee, int installMaxPerDay, int installMaxDaysAhead) {
}
