package com.nguyen.motoparts.aftersales.controller;

import com.nguyen.motoparts.aftersales.dto.AdminWarrantyDetail;
import com.nguyen.motoparts.aftersales.dto.AdminWarrantyRow;
import com.nguyen.motoparts.aftersales.dto.WarrantyDecisionRequest;
import com.nguyen.motoparts.aftersales.service.AdminWarrantyService;
import com.nguyen.motoparts.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Quản trị yêu cầu bảo hành. Chỉ ADMIN (SecurityConfig chặn /api/admin/**). */
@RestController
@RequestMapping("/api/admin/warranty-requests")
public class AdminWarrantyController {

    private final AdminWarrantyService service;

    public AdminWarrantyController(AdminWarrantyService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AdminWarrantyRow> list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.list(status, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    @GetMapping("/{requestId}")
    public AdminWarrantyDetail detail(@PathVariable String requestId) {
        return service.detail(requestId);
    }

    @PostMapping("/{requestId}/decide")
    public AdminWarrantyDetail decide(@PathVariable String requestId, @Valid @RequestBody WarrantyDecisionRequest request) {
        return service.decide(requestId, request.approve(), request.note());
    }
}
