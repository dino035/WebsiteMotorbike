package com.nguyen.motoparts.catalog.controller;

import com.nguyen.motoparts.catalog.dto.ConditionAssessmentRequest;
import com.nguyen.motoparts.catalog.dto.MotorbikeRequest;
import com.nguyen.motoparts.catalog.dto.ProductDetailResponse;
import com.nguyen.motoparts.catalog.service.MotorbikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/{productId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDetailResponse addPhoto(@PathVariable String productId,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "caption", required = false) String caption) {
        return motorbikeService.addPhoto(productId, file, caption);
    }

    @DeleteMapping("/{productId}/photos/{photoId}")
    public ProductDetailResponse deletePhoto(@PathVariable String productId,
                                             @PathVariable String photoId) {
        return motorbikeService.deletePhoto(productId, photoId);
    }
}