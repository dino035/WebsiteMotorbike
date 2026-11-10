package com.nguyen.motoparts.account.controller;

import com.nguyen.motoparts.account.dto.AddressRequest;
import com.nguyen.motoparts.account.dto.AddressResponse;
import com.nguyen.motoparts.account.service.AddressService;
import com.nguyen.motoparts.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Sổ địa chỉ của chính người đang đăng nhập (accountId lấy từ token, không nhận từ client). */
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal AuthUser user) {
        return service.list(user.accountId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AddressRequest request) {
        return service.create(user.accountId(), request);
    }

    @PutMapping("/{addressId}")
    public AddressResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable String addressId,
                                  @Valid @RequestBody AddressRequest request) {
        return service.update(user.accountId(), addressId, request);
    }

    @PostMapping("/{addressId}/default")
    public AddressResponse setDefault(@AuthenticationPrincipal AuthUser user, @PathVariable String addressId) {
        return service.setDefault(user.accountId(), addressId);
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable String addressId) {
        service.delete(user.accountId(), addressId);
    }
}
