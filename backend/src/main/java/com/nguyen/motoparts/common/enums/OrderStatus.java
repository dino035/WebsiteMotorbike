package com.nguyen.motoparts.common.enums;

import lombok.Getter;

import java.util.List;

/** Trạng thái đơn hàng (SRS 3.12, 3.21). Giữ nguyên tên hằng số như WebsiteMotorbike. */
@Getter
public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao/Chờ lấy"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** Trạng thái Admin được chuyển tiếp từ trạng thái này. COMPLETED và CANCELLED là trạng thái cuối. */
    public List<OrderStatus> getNextStatuses() {
        return switch (this) {
            case PENDING -> List.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> List.of(SHIPPING, CANCELLED);
            case SHIPPING -> List.of(COMPLETED, CANCELLED);
            default -> List.of();
        };
    }

    public boolean canMoveTo(OrderStatus next) {
        return getNextStatuses().contains(next);
    }
}
