package com.nguyen.motoparts.order.controller;

import com.nguyen.motoparts.order.dto.CheckoutPolicyResponse;
import com.nguyen.motoparts.order.dto.CheckoutRequest;
import com.nguyen.motoparts.order.dto.OrderDetailResponse;
import com.nguyen.motoparts.order.dto.OrderSummaryResponse;
import com.nguyen.motoparts.order.dto.PaymentLinkResponse;
import com.nguyen.motoparts.order.service.OnlinePaymentService;
import com.nguyen.motoparts.order.service.OrderService;
import com.nguyen.motoparts.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Đơn hàng của chính người đang đăng nhập (accountId lấy từ token, không nhận từ client). */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;
    private final OnlinePaymentService onlinePayments;

    public OrderController(OrderService service, OnlinePaymentService onlinePayments) {
        this.service = service;
        this.onlinePayments = onlinePayments;
    }

    /** Quy tắc cho trang đặt hàng: phí công và giới hạn ngày hẹn lắp đặt. */
    @GetMapping("/policy")
    public CheckoutPolicyResponse policy() {
        return service.policy();
    }

    /** Đặt hàng từ giỏ hiện tại. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse place(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CheckoutRequest request) {
        return service.placeOrder(user.accountId(), request);
    }

    @GetMapping
    public List<OrderSummaryResponse> list(@AuthenticationPrincipal AuthUser user) {
        return service.listMine(user.accountId());
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse detail(@AuthenticationPrincipal AuthUser user, @PathVariable String orderId) {
        return service.getMine(user.accountId(), orderId);
    }

    /** Bắt đầu (hoặc thử lại) một lần trả tiền VNPay cho đơn; trả liên kết để React chuyển khách sang cổng VNPay. */
    @PostMapping("/{orderId}/vnpay")
    public PaymentLinkResponse payWithVnpay(@AuthenticationPrincipal AuthUser user, @PathVariable String orderId,
                                            HttpServletRequest request) {
        return onlinePayments.startAttempt(user.accountId(), orderId, clientIp(request));
    }

    /** Đổi đơn VNPay chưa trả được tiền sang thanh toán khi nhận hàng (COD). */
    @PostMapping("/{orderId}/pay-cod")
    public OrderDetailResponse payWithCod(@AuthenticationPrincipal AuthUser user, @PathVariable String orderId) {
        return onlinePayments.useCod(user.accountId(), orderId);
    }

    /** Địa chỉ IP gửi cho VNPay; không tin tiêu đề X-Forwarded-For vì người dùng tự đặt được. */
    private static String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip == null || ip.contains(":") ? "127.0.0.1" : ip;
    }
}
