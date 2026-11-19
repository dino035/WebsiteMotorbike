package com.nguyen.motoparts.order.dto;

/**
 * Kết quả cho trang "quay về từ VNPay" của React. outcome là tên Outcome của OnlinePaymentService; paid cho biết
 * đơn đang ở trạng thái đã thanh toán sau lần này; message là câu tiếng Việt hiển thị cho khách.
 */
public record VnpayReturnResponse(String outcome, boolean paid, String message, String orderId, String orderCode,
                                  String reason) {
}
