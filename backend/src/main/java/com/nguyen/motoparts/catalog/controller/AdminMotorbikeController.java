package com.nguyen.motoparts.catalog.controller;

import com.nguyen.motoparts.catalog.dto.ConditionAssessmentRequest;
import com.nguyen.motoparts.catalog.dto.MotorbikeRequest;
import com.nguyen.motoparts.catalog.dto.ProductDetailResponse;
import com.nguyen.motoparts.catalog.service.MotorbikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/motorbikes")
@RequiredArgsConstructor
public class AdminMotorbikeController {

    private final MotorbikeService motorbikeService;

    @PostMapping
    public ProductDetailResponse create(@Valid @RequestBody MotorbikeRequest req) {
        return motorbikeService.create(req);
    }

    @PutMapping("/{productId}")
    public ProductDetailResponse update(@PathVariable String productId,
                                        @Valid @RequestBody MotorbikeRequest req) {
        return motorbikeService.update(productId, req);
    }

    @DeleteMapping("/{productId}")
    public void delete(@PathVariable String productId) {
        motorbikeService.delete(productId);
    }

    @PutMapping("/{productId}/condition")
    public ProductDetailResponse saveAssessment(@PathVariable String productId,
                                                @Valid @RequestBody ConditionAssessmentRequest req) {
        return motorbikeService.saveAssessment(productId, req);
    }
}