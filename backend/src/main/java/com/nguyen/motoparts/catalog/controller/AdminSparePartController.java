package com.nguyen.motoparts.catalog.controller;

import com.nguyen.motoparts.catalog.dto.ProductDetailResponse;
import com.nguyen.motoparts.catalog.dto.SparePartRequest;
import com.nguyen.motoparts.catalog.service.SparePartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/spare-parts")
@RequiredArgsConstructor
public class AdminSparePartController {

    private final SparePartService sparePartService;

    @PostMapping
    public ProductDetailResponse create(@Valid @RequestBody SparePartRequest req) {
        return sparePartService.create(req);
    }

    @PutMapping("/{productId}")
    public ProductDetailResponse update(@PathVariable String productId,
                                        @Valid @RequestBody SparePartRequest req) {
        return sparePartService.update(productId, req);
    }

    @DeleteMapping("/{productId}")
    public void delete(@PathVariable String productId) {
        sparePartService.delete(productId);
    }
}