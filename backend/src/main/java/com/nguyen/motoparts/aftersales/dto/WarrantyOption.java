package com.nguyen.motoparts.aftersales.dto;

import java.time.LocalDateTime;

/**
 * Một sản phẩm khách đã mua (đơn Hoàn thành) kèm tình trạng bảo hành, dùng để hiển thị danh sách chọn
 * khi gửi yêu cầu bảo hành. KHÔNG phải entity (không lưu CSDL) nên không vẽ trong class diagram.
 * canRequest: còn hạn và chưa có yêu cầu nào đang chờ xử lý thì được gửi yêu cầu mới.
 */
public record WarrantyOption(
        String orderItemId,
        String productName,
        String orderCode,
        LocalDateTime purchasedAt,
        LocalDateTime deadline,
        boolean expired,
        boolean pending,
        boolean canRequest) {
}
