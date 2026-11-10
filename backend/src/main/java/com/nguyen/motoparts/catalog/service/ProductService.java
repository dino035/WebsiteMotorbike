package com.nguyen.motoparts.catalog.service;

import com.nguyen.motoparts.catalog.dto.ProductSummaryResponse;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.catalog.repository.ProductSpecifications;
import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.common.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public PageResponse<ProductSummaryResponse> search(String keyword, String type, ProductStatus status,
                                                       BigDecimal minPrice, BigDecimal maxPrice,
                                                       int page, int size) {

        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.nameContains(keyword),
                ProductSpecifications.ofType(type),
                ProductSpecifications.hasStatus(status),
                ProductSpecifications.priceAtLeast(minPrice),
                ProductSpecifications.priceAtMost(maxPrice)
        );

        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));

        Page<ProductSummaryResponse> result = productRepository.findAll(spec, pageable)
                .map(ProductSummaryResponse::from);
        return PageResponse.from(result);
    }

}
