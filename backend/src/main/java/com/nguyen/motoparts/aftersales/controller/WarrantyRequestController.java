package com.nguyen.motoparts.aftersales.controller;

import com.nguyen.motoparts.aftersales.dto.CreateWarrantyRequest;
import com.nguyen.motoparts.aftersales.dto.WarrantyOption;
import com.nguyen.motoparts.aftersales.dto.WarrantyRequestResponse;
import com.nguyen.motoparts.aftersales.service.WarrantyRequestService;
import com.nguyen.motoparts.security.AuthUser;
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

/** Bảo hành của chính người đang đăng nhập (accountId lấy từ token, không nhận từ client). */
@RestController
@RequestMapping("/api/warranty-requests")
public class WarrantyRequestController {

    private final WarrantyRequestService service;

    public WarrantyRequestController(WarrantyRequestService service) {
        this.service = service;
    }

    /** Sản phẩm đã mua (đơn Hoàn thành) kèm hạn bảo hành và có gửi yêu cầu được không. */
    @GetMapping("/options")
    public List<WarrantyOption> options(@AuthenticationPrincipal AuthUser user) {
        return service.options(user.accountId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarrantyRequestResponse create(@AuthenticationPrincipal AuthUser user,
                                          @Valid @RequestBody CreateWarrantyRequest request) {
        return service.create(user.accountId(), request);
    }

    @GetMapping
    public List<WarrantyRequestResponse> list(@AuthenticationPrincipal AuthUser user) {
        return service.listMine(user.accountId());
    }

    @GetMapping("/{requestId}")
    public WarrantyRequestResponse detail(@AuthenticationPrincipal AuthUser user, @PathVariable String requestId) {
        return service.getMine(user.accountId(), requestId);
    }
}
