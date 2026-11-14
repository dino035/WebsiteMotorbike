package com.nguyen.motoparts.order.dto;

import com.nguyen.motoparts.common.enums.DeliveryMethod;
import com.nguyen.motoparts.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Đặt hàng từ giỏ của người đang đăng nhập. Người nhận và địa chỉ được CHỤP vào đơn (khách sửa hay xóa sổ địa chỉ
 * sau đó thì đơn cũ không đổi), nên React chọn địa chỉ trong sổ rồi gửi nội dung ở đây.
 */
public record CheckoutRequest(
        @NotNull(message = "Vui lòng chọn hình thức nhận hàng.")
        DeliveryMethod deliveryMethod,

        @NotBlank(message = "Vui lòng nhập họ tên người nhận.")
        @Size(max = 100, message = "Họ tên người nhận quá dài (tối đa 100 ký tự).")
        String receiverName,

        @NotBlank(message = "Vui lòng nhập số điện thoại.")
        String receiverPhone,

        /** Bắt buộc khi giao tận nơi; nhận tại cửa hàng và lắp đặt thì bỏ trống. */
        @Size(max = 500, message = "Địa chỉ giao hàng quá dài (tối đa 500 ký tự).")
        String shippingAddress,

        @NotNull(message = "Vui lòng chọn phương thức thanh toán.")
        PaymentMethod paymentMethod,

        /** Khách đã đọc và đồng ý chính sách miễn đổi trả; phải là true. */
        boolean returnPolicyAccepted,

        /** Ngày hẹn lắp đặt (yyyy-MM-dd), chỉ dùng khi deliveryMethod là INSTALL. */
        LocalDate installDate) {
}
