package com.nguyen.motoparts.order.dto;

import java.math.BigDecimal;

/**
 * Các quy tắc React cần để vẽ trang đặt hàng (phí công, giới hạn ngày hẹn lắp đặt, VNPay đã được cấu hình hay chưa,
 * số phút để trả tiền VNPay trước khi đơn tự hủy).
 */
public record CheckoutPolicyResponse(BigDecimal laborFee, int installMaxPerDay, int installMaxDaysAhead,
                                     boolean vnpayEnabled, int vnpayHoldMinutes) {
}
