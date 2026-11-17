package com.nguyen.motoparts.catalog.service;

import com.nguyen.motoparts.catalog.dto.ProductDetailResponse;
import com.nguyen.motoparts.catalog.dto.SparePartRequest;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.repository.SparePartRepository;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.compatibility.repository.CompatibilityRuleRepository;
import com.nguyen.motoparts.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SparePartService {

    private final SparePartRepository sparePartRepository;
    private final OrderItemRepository orderItemRepository;
    private final CompatibilityRuleRepository ruleRepository;

    @Transactional
    public ProductDetailResponse create(SparePartRequest req) {
        SparePart part = new SparePart();
        apply(req, part);
        return ProductDetailResponse.from(sparePartRepository.save(part));
    }

    @Transactional
    public ProductDetailResponse update(String productId, SparePartRequest req) {
        SparePart part = sparePartRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phụ tùng."));
        apply(req, part);
        return ProductDetailResponse.from(part);
    }

    @Transactional
    public void delete(String productId) {
        SparePart part = sparePartRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phụ tùng."));

        if (orderItemRepository.existsByProductId(productId)) {
            throw new ConflictException(
                    "Phụ tùng này đã có trong đơn hàng nên không xóa được. "
                            + "Bạn có thể đổi tình trạng sang Đã bán.");
        }

        ruleRepository.deleteBySparePart_ProductId(productId);
        sparePartRepository.delete(part);
    }

    /** Chép dữ liệu từ request sang entity. Dùng chung cho thêm mới và sửa. */
    private void apply(SparePartRequest req, SparePart part) {
        part.setName(req.name());
        part.setPrice(req.price());
        part.setStatus(req.status());
        part.setPartType(req.partType());
        part.setPartOrigin(req.partOrigin());
    }
}