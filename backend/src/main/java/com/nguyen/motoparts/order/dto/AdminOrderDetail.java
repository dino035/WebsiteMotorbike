package com.nguyen.motoparts.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chi tiết đơn cho Admin: nội dung đơn như khách thấy, thông tin khách, các trạng thái được chuyển tiếp và
 * có được bấm "Xác nhận đã thu tiền" (COD, chưa thu, đơn chưa hủy) hay không, và các lần trả tiền VNPay (mới nhất trước;
 * Admin cần mã giao dịch để đối soát và hoàn tiền khi khách trả tiền cho đơn đã hủy).
 */
public record AdminOrderDetail(
        OrderDetailResponse order,
        Customer customer,
        List<NextStatus> nextStatuses,
        boolean canConfirmPayment,
        List<OnlineAttempt> onlinePayments) {

    public record Customer(String accountId, String email, String phone) {
    }

    public record NextStatus(String status, String label) {
    }

    public record OnlineAttempt(String txnRef, BigDecimal amount, String status, String statusLabel, String responseCode,
                                String transactionNo, String bankCode, LocalDateTime createdAt,
                                LocalDateTime completedAt) {
    }
}
