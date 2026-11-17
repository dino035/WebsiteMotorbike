package com.nguyen.motoparts.catalog.controller;

import com.nguyen.motoparts.catalog.dto.ProductDetailResponse;
import com.nguyen.motoparts.catalog.dto.SparePartRequest;
import com.nguyen.motoparts.catalog.service.SparePartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/{productId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDetailResponse addPhoto(@PathVariable String productId,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "caption", required = false) String caption) {
        return sparePartService.addPhoto(productId, file, caption);
    }

    @DeleteMapping("/{productId}/photos/{photoId}")
    public ProductDetailResponse deletePhoto(@PathVariable String productId,
                                             @PathVariable String photoId) {
        return sparePartService.deletePhoto(productId, photoId);
    }
}