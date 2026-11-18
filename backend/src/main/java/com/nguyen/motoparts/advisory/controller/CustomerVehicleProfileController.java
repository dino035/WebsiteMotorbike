package com.nguyen.motoparts.advisory.controller;

import com.nguyen.motoparts.advisory.dto.VehicleProfileRequest;
import com.nguyen.motoparts.advisory.dto.VehicleProfileResponse;
import com.nguyen.motoparts.advisory.service.CustomerVehicleProfileService;
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

/** Hồ sơ xe của chính người đang đăng nhập (accountId lấy từ token). */
@RestController
@RequestMapping("/api/vehicle-profiles")
public class CustomerVehicleProfileController {

    private final CustomerVehicleProfileService service;

    public CustomerVehicleProfileController(CustomerVehicleProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<VehicleProfileResponse> list(@AuthenticationPrincipal AuthUser user) {
        return service.listMine(user.accountId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleProfileResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody VehicleProfileRequest request) {
        return service.create(user.accountId(), request);
    }

    @PutMapping("/{profileId}")
    public VehicleProfileResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable String profileId,
                                         @Valid @RequestBody VehicleProfileRequest request) {
        return service.update(user.accountId(), profileId, request);
    }

    @DeleteMapping("/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable String profileId) {
        service.delete(user.accountId(), profileId);
    }
}
