package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Một lần trả tiền qua cổng VNPay cho một đơn hàng. Một đơn có thể có nhiều lần (trả lỗi rồi thử lại).
 * txnRef là mã giao dịch gửi cho VNPay (vnp_TxnRef), mỗi lần một mã mới, không đoán được.
 * Chỉ giữ orderId và accountId kiểu String, không nối quan hệ (quy ước README mục 3).
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class OnlinePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String attemptId;

    private String orderId;

    private String accountId;

    @Column(length = 64, unique = true)
    private String txnRef;

    /** Số tiền VND của lần thanh toán này (bằng Payment.amount lúc bắt đầu). */
    private BigDecimal amount;

    /** PENDING: đang chờ kết quả; PAID: thành công; FAILED: thất bại, bị hủy hoặc hết hạn. */
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /** vnp_ResponseCode do VNPay trả về. */
    private String responseCode;

    /** vnp_TransactionNo: mã giao dịch phía VNPay. */
    private String transactionNo;

    private String bankCode;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
