package com.nguyen.motoparts.aftersales.dto;

import java.time.LocalDateTime;

/** Chi tiết yêu cầu bảo hành cho Admin: yêu cầu, khách, ngày mua và hạn bảo hành, có được quyết định hay không. */
public record AdminWarrantyDetail(
        WarrantyRequestResponse request,
        Customer customer,
        LocalDateTime purchasedAt,
        LocalDateTime deadline,
        /** Chỉ yêu cầu đang chờ xử lý mới duyệt hoặc từ chối được. */
        boolean canDecide) {

    public record Customer(String accountId, String email, String phone) {
    }
}
