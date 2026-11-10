package com.nguyen.motoparts.catalog.controller;

import com.nguyen.motoparts.catalog.dto.ProductDetailResponse;
import com.nguyen.motoparts.catalog.dto.ProductSummaryResponse;
import com.nguyen.motoparts.catalog.service.ProductService;
import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.common.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductSummaryResponse> list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "status", required = false) ProductStatus status,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {

        // chặn giá trị xấu: page không âm, size trong khoảng 1..50
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        return productService.search(keyword, type, status, minPrice, maxPrice, safePage, safeSize);
    }

    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable("id") String id) {
        return productService.getDetail(id);
    }
}