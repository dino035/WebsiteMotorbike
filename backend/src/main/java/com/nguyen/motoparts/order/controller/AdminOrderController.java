package com.nguyen.motoparts.order.controller;

import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.order.dto.AdminOrderDetail;
import com.nguyen.motoparts.order.dto.AdminOrderRow;
import com.nguyen.motoparts.order.dto.InstallationActionRequest;
import com.nguyen.motoparts.order.dto.InstallationRow;
import com.nguyen.motoparts.order.dto.StatusChangeRequest;
import com.nguyen.motoparts.order.service.AdminInstallationService;
import com.nguyen.motoparts.order.service.AdminOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Quản trị đơn hàng và lịch lắp đặt. Chỉ ADMIN (SecurityConfig chặn /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class AdminOrderController {

    private final AdminOrderService orders;
    private final AdminInstallationService installations;

    public AdminOrderController(AdminOrderService orders, AdminInstallationService installations) {
        this.orders = orders;
        this.installations = installations;
    }

    @GetMapping("/orders")
    public PageResponse<AdminOrderRow> listOrders(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return orders.list(status, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    @GetMapping("/orders/{orderId}")
    public AdminOrderDetail orderDetail(@PathVariable String orderId) {
        return orders.detail(orderId);
    }

    @PostMapping("/orders/{orderId}/status")
    public AdminOrderDetail changeStatus(@PathVariable String orderId, @Valid @RequestBody StatusChangeRequest request) {
        return orders.changeStatus(orderId, request.status());
    }

    @PostMapping("/orders/{orderId}/confirm-payment")
    public AdminOrderDetail confirmPayment(@PathVariable String orderId) {
        return orders.confirmPayment(orderId);
    }

    @GetMapping("/installations")
    public List<InstallationRow> listInstallations(@RequestParam(name = "status", required = false) String status) {
        return installations.list(status);
    }

    @PostMapping("/installations/{requestId}/action")
    public InstallationRow installationAction(@PathVariable String requestId,
                                              @Valid @RequestBody InstallationActionRequest request) {
        return installations.act(requestId, request);
    }
}
