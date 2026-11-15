package com.nguyen.motoparts.order.dto;

import java.util.List;

/**
 * Chi tiết đơn cho Admin: nội dung đơn như khách thấy, thông tin khách, các trạng thái được chuyển tiếp và
 * có được bấm "Xác nhận đã thu tiền" (COD, chưa thu, đơn chưa hủy) hay không.
 */
public record AdminOrderDetail(
        OrderDetailResponse order,
        Customer customer,
        List<NextStatus> nextStatuses,
        boolean canConfirmPayment) {

    public record Customer(String accountId, String email, String phone) {
    }

    public record NextStatus(String status, String label) {
    }
}
