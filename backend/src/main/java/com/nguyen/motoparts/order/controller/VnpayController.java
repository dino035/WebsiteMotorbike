package com.nguyen.motoparts.order.controller;

import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.order.dto.VnpayReturnResponse;
import com.nguyen.motoparts.order.service.OnlinePaymentService;
import com.nguyen.motoparts.order.service.OnlinePaymentService.Outcome;
import com.nguyen.motoparts.order.service.OnlinePaymentService.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Hai đường VNPay báo kết quả về. Cả hai KHÔNG đòi đăng nhập: kết quả được tin nhờ chữ ký HMAC-SHA512, không nhờ
 * phiên đăng nhập (xem SecurityConfig).
 */
@RestController
@RequestMapping("/api/payments/vnpay")
public class VnpayController {

    private final OnlinePaymentService service;

    public VnpayController(OnlinePaymentService service) {
        this.service = service;
    }

    /** Trình duyệt của khách quay về trang React, trang đó gửi nguyên các tham số vnp_* sang đây để ghi nhận và hiển thị. */
    @GetMapping("/return")
    public VnpayReturnResponse paymentReturn(@RequestParam Map<String, String> query) {
        Result result = service.process(vnpParams(query));
        return switch (result.outcome()) {
            case NOT_CONFIGURED -> throw new BadRequestException("Hệ thống chưa cấu hình VNPay.");
            case INVALID_SIGNATURE -> throw new BadRequestException(
                    "Chữ ký không hợp lệ, không thể xác nhận kết quả thanh toán.");
            default -> new VnpayReturnResponse(result.outcome().name(), result.attemptPaid(), message(result),
                    result.orderId(), result.orderCode(), result.reason());
        };
    }

    /**
     * IPN: VNPay gọi thẳng máy chủ để báo kết quả, kể cả khi khách đóng trình duyệt trước khi quay về. Phải trả JSON
     * {RspCode, Message} theo quy định của VNPay: 00 là đã nhận, các mã khác báo lỗi.
     */
    @GetMapping("/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> query) {
        Outcome outcome = service.process(vnpParams(query)).outcome();
        String[] answer = switch (outcome) {
            case NOT_CONFIGURED -> new String[]{"99", "Not configured"};
            case INVALID_SIGNATURE -> new String[]{"97", "Invalid signature"};
            case NOT_FOUND -> new String[]{"01", "Order not found"};
            case AMOUNT_MISMATCH -> new String[]{"04", "Invalid amount"};
            case ALREADY_PROCESSED -> new String[]{"02", "Order already confirmed"};
            default -> new String[]{"00", "Confirm Success"};
        };
        return Map.of("RspCode", answer[0], "Message", answer[1]);
    }

    /** Chỉ giữ các tham số vnp_* mà VNPay gửi về. */
    private static Map<String, String> vnpParams(Map<String, String> query) {
        Map<String, String> params = new HashMap<>();
        query.forEach((key, value) -> {
            if (key.startsWith("vnp_")) {
                params.put(key, value);
            }
        });
        return params;
    }

    /** Khách quay về sau khi IPN đã ghi nhận (hay gặp khi chạy thật): vẫn báo đúng kết quả của lần trả tiền đó. */
    private static String message(Result result) {
        return switch (result.outcome()) {
            case PAID -> "Thanh toán thành công. Cảm ơn bạn!";
            case FAILED -> "Thanh toán không thành công. Bạn có thể thanh toán lại hoặc chọn thanh toán khi nhận hàng.";
            case ALREADY_PROCESSED -> result.attemptPaid()
                    ? "Thanh toán thành công. Cảm ơn bạn!"
                    : "Thanh toán không thành công. Bạn có thể thanh toán lại hoặc chọn thanh toán khi nhận hàng.";
            case AMOUNT_MISMATCH -> "Số tiền thanh toán không khớp với đơn hàng, giao dịch không được ghi nhận.";
            case NOT_FOUND -> "Không tìm thấy giao dịch thanh toán này.";
            case PAID_ORDER_CANCELLED, PAID_DUPLICATE ->
                    "Cửa hàng đã nhận tiền nhưng không thể áp dụng cho đơn hàng; cửa hàng sẽ liên hệ để hoàn tiền.";
            default -> "Không xác nhận được kết quả thanh toán.";
        };
    }
}
