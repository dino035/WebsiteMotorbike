package com.nguyen.motoparts.order.dto;

/** Liên kết chuyển khách sang cổng VNPay cho một lần thanh toán; txnRef là mã giao dịch của lần đó. */
public record PaymentLinkResponse(String paymentUrl, String txnRef) {
}
