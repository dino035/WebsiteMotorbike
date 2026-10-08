package com.example.websitemotorbike.model;

/**
 * Một sản phẩm khách đã mua (đơn Hoàn thành) kèm thông tin bảo hành để hiển thị.
 * Không phải entity, chỉ dùng cho view.
 */
public class WarrantyOption {

    private final String orderItemId;
    private final String productName;
    private final String orderCode;
    private final String purchasedText;
    private final String deadlineText;
    private final boolean expired;
    private final boolean pending;

    public WarrantyOption(String orderItemId, String productName, String orderCode,
                          String purchasedText, String deadlineText, boolean expired, boolean pending) {
        this.orderItemId = orderItemId;
        this.productName = productName;
        this.orderCode = orderCode;
        this.purchasedText = purchasedText;
        this.deadlineText = deadlineText;
        this.expired = expired;
        this.pending = pending;
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public String getProductName() {
        return productName;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getPurchasedText() {
        return purchasedText;
    }

    public String getDeadlineText() {
        return deadlineText;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isPending() {
        return pending;
    }

    /** Còn hạn và chưa có yêu cầu nào đang chờ xử lý thì được gửi yêu cầu mới. */
    public boolean isCanRequest() {
        return !expired && !pending;
    }
}
