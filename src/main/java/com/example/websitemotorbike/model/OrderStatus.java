package com.example.websitemotorbike.model;

import lombok.Getter;

import java.util.List;

/** Trạng thái xử lý đơn hàng (SRS 3.12). Không đổi tên hằng số vì CSDL lưu chuỗi này. */
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

    /**
     * Các trạng thái Admin được phép chuyển tiếp từ trạng thái này (SRS 3.21):
     * chờ xác nhận -> đã xác nhận -> đang giao/chờ lấy -> hoàn thành; hủy được cho tới khi hoàn thành.
     * COMPLETED và CANCELLED là trạng thái cuối.
     */
    public List<OrderStatus> getNextStatuses() {
        switch (this) {
            case PENDING:
                return List.of(CONFIRMED, CANCELLED);
            case CONFIRMED:
                return List.of(SHIPPING, CANCELLED);
            case SHIPPING:
                return List.of(COMPLETED, CANCELLED);
            default:
                return List.of();
        }
    }

    public boolean canMoveTo(OrderStatus next) {
        return getNextStatuses().contains(next);
    }
}
