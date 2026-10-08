package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Yêu cầu lắp đặt phụ tùng tại cửa hàng gắn với một đơn hàng (SRS 3.11).
 * Mỗi đơn có tối đa một yêu cầu. laborFee là phí công đã cộng vào tổng tiền của đơn.
 * Chỉ giữ accountId kiểu String, không đặt @ManyToOne User (quy ước README mục 3).
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class InstallationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String installationId;

    private String accountId;

    @OneToOne(optional = false)
    @JoinColumn(name = "orderId")
    private Order order;

    private LocalDate scheduledDate;

    private BigDecimal laborFee;

    @Enumerated(EnumType.STRING)
    private InstallationStatus status;

    private LocalDateTime createdAt;

    /** Ngày hẹn dạng dd/MM/yyyy để hiển thị trên JSP. */
    public String getScheduledDateText() {
        return scheduledDate == null ? "" : scheduledDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /** Ngày hẹn dạng yyyy-MM-dd, dùng làm giá trị cho ô chọn ngày (input type=date). */
    public String getScheduledDateIso() {
        return scheduledDate == null ? "" : scheduledDate.toString();
    }
}
