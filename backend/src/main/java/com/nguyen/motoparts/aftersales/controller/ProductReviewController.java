package com.nguyen.motoparts.aftersales.controller;

import com.nguyen.motoparts.aftersales.dto.CreateReviewRequest;
import com.nguyen.motoparts.aftersales.dto.ReviewResponse;
import com.nguyen.motoparts.aftersales.dto.ReviewSummaryResponse;
import com.nguyen.motoparts.aftersales.service.ProductReviewService;
import com.nguyen.motoparts.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;

    /** Công khai: khách chưa đăng nhập vẫn xem được đánh giá. */
    @GetMapping
    public ReviewSummaryResponse list(@PathVariable String productId) {
        return reviewService.listByProduct(productId);
    }

    /** Cần đăng nhập; service kiểm tra tiếp là đã mua và chưa đánh giá. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal AuthUser user,
                                 @PathVariable String productId,
                                 @Valid @RequestBody CreateReviewRequest req) {
        return reviewService.create(user.accountId(), productId, req);
    }
}