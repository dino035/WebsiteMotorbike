package com.nguyen.motoparts.aftersales.service;

import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.aftersales.dto.CreateReviewRequest;
import com.nguyen.motoparts.aftersales.dto.ReviewResponse;
import com.nguyen.motoparts.aftersales.dto.ReviewSummaryResponse;
import com.nguyen.motoparts.aftersales.entity.ProductReview;
import com.nguyen.motoparts.aftersales.repository.ProductReviewRepository;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Đánh giá sản phẩm: chỉ người đã mua (đơn Hoàn thành) mới gửi được, mỗi người một lần cho mỗi sản phẩm. */
@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse create(String accountId, String productId, CreateReviewRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm."));

        if (!orderItemRepository.existsByProductIdAndOrder_AccountIdAndOrder_Status(
                productId, accountId, OrderStatus.COMPLETED)) {
            throw new ConflictException("Bạn chỉ có thể đánh giá sản phẩm đã mua.");
        }

        if (reviewRepository.existsByAccountIdAndProduct_ProductId(accountId, productId)) {
            throw new ConflictException("Bạn đã đánh giá sản phẩm này rồi.");
        }

        ProductReview review = new ProductReview();
        review.setAccountId(accountId);
        review.setProduct(product);
        review.setRating(req.rating());
        review.setComment(req.comment().trim());
        reviewRepository.save(review);

        String email = userRepository.findById(accountId).map(User::getEmail).orElse(null);
        return ReviewResponse.from(review, email);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse listByProduct(String productId) {
        List<ProductReview> reviews = reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(productId);
        if (reviews.isEmpty()) {
            return new ReviewSummaryResponse(0, 0, List.of());
        }

        // Lấy email của mọi người đánh giá trong MỘT câu truy vấn, thay vì mỗi dòng một câu.
        Map<String, String> emailById = userRepository
                .findAllById(reviews.stream().map(ProductReview::getAccountId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getAccountId, User::getEmail));

        List<ReviewResponse> items = reviews.stream()
                .map(r -> ReviewResponse.from(r, emailById.get(r.getAccountId())))
                .toList();

        double average = reviews.stream().mapToInt(ProductReview::getRating).average().orElse(0);
        return new ReviewSummaryResponse(Math.round(average * 10) / 10.0, reviews.size(), items);
    }
}