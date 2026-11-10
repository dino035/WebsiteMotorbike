package com.nguyen.motoparts.account.controller;

import com.nguyen.motoparts.account.dto.AccountRow;
import com.nguyen.motoparts.account.dto.LockRequest;
import com.nguyen.motoparts.account.service.AccountAdminService;
import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Quản trị tài khoản khách hàng. Chỉ ADMIN (SecurityConfig chặn /api/admin/**). */
@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AccountAdminService service;

    public AdminAccountController(AccountAdminService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AccountRow> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.list(q, status, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    @PostMapping("/{accountId}/lock")
    public AccountRow lock(@AuthenticationPrincipal AuthUser admin, @PathVariable String accountId,
                           @Valid @RequestBody(required = false) LockRequest request) {
        LockRequest r = request == null ? new LockRequest(null, false) : request;
        return service.lock(admin.accountId(), accountId, r.reason(), r.force());
    }

    @PostMapping("/{accountId}/unlock")
    public AccountRow unlock(@PathVariable String accountId) {
        return service.unlock(accountId);
    }
}
