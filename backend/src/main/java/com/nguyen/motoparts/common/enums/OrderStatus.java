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

    /**
     * Nhãn hiển thị cho một đơn cụ thể. SHIPPING là một giai đoạn duy nhất trong máy trạng thái (hàng đã rời kho,
     * chưa tới tay khách), nhưng gọi khác nhau theo cách nhận hàng: giao tận nơi là "Đang giao", còn nhận hoặc
     * lắp tại cửa hàng là "Chờ lấy". Đơn không rõ cách nhận (nhập tay, dữ liệu cũ) giữ nhãn chung.
     */
    public String labelFor(DeliveryMethod method) {
        if (this != SHIPPING || method == null) {
            return label;
        }
        return method == DeliveryMethod.DELIVERY ? "Đang giao" : "Chờ lấy";
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
