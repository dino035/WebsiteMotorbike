package com.nguyen.motoparts.order.entity;

import com.nguyen.motoparts.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một lần trả tiền qua VNPay cho một đơn; một đơn có thể có nhiều lần (lỗi rồi thử lại).
 * txnRef là vnp_TxnRef gửi cho VNPay, mỗi lần một mã mới.
 * Giữ từ WebsiteMotorbike (class ngoài đặc tả, sẽ bổ sung vào class diagram).
 */
@Entity
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

    private BigDecimal amount;

    /** PENDING: chờ kết quả; PAID: thành công; FAILED: thất bại, bị hủy hoặc hết hạn. */
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /** vnp_ResponseCode. */
    private String responseCode;

    /** vnp_TransactionNo. */
    private String transactionNo;

    private String bankCode;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
