package com.nguyen.motoparts.catalog.service;

import com.nguyen.motoparts.catalog.dto.ProductDetailResponse;
import com.nguyen.motoparts.catalog.dto.SparePartRequest;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.entity.SparePartPhoto;
import com.nguyen.motoparts.catalog.repository.SparePartRepository;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.common.storage.PhotoStorageService;
import com.nguyen.motoparts.compatibility.repository.CompatibilityRuleRepository;
import com.nguyen.motoparts.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SparePartService {

    private final SparePartRepository sparePartRepository;
    private final OrderItemRepository orderItemRepository;
    private final CompatibilityRuleRepository ruleRepository;
    private final PhotoStorageService photoStorage;

    @Transactional
    public ProductDetailResponse create(SparePartRequest req) {
        SparePart part = new SparePart();
        requireReadyToSell(part, req.status());
        apply(req, part);
        return ProductDetailResponse.from(sparePartRepository.save(part));
    }

    @Transactional
    public ProductDetailResponse update(String productId, SparePartRequest req) {
        SparePart part = sparePartRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phụ tùng."));

        requireReadyToSell(part, req.status());
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
        part.getPhotos().forEach(p -> photoStorage.delete(p.getStoragePath()));
        sparePartRepository.delete(part);
    }

    @Transactional
    public ProductDetailResponse addPhoto(String productId, MultipartFile file, String caption) {
        SparePart part = sparePartRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phụ tùng."));

        if (part.getPhotos().size() >= PhotoStorageService.MAX_PHOTOS) {
            throw new ConflictException(
                    "Mỗi phụ tùng chỉ được tối đa " + PhotoStorageService.MAX_PHOTOS
                            + " ảnh. Hãy xóa bớt ảnh cũ trước khi thêm ảnh mới.");
        }

        String storagePath = photoStorage.upload(file, "spareparts");

        SparePartPhoto photo = new SparePartPhoto();
        photo.setStoragePath(storagePath);
        photo.setImageUrl(photoStorage.publicUrl(storagePath));
        photo.setCaption(caption);
        part.addPhoto(photo);
        sparePartRepository.flush();

        return ProductDetailResponse.from(part);
    }

    @Transactional
    public ProductDetailResponse deletePhoto(String productId, String photoId) {
        SparePart part = sparePartRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phụ tùng."));

        SparePartPhoto photo = part.getPhotos().stream()
                .filter(p -> p.getPhotoId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh của phụ tùng này."));

        if (part.getStatus() == ProductStatus.AVAILABLE && part.getPhotos().size() <= 1) {
            throw new ConflictException(
                    "Phụ tùng đang bán phải có ít nhất 1 ảnh. "
                            + "Hãy thêm ảnh khác, hoặc đổi tình trạng sang Hết hàng trước khi xóa.");
        }

        part.removePhoto(photo);
        photoStorage.delete(photo.getStoragePath());

        return ProductDetailResponse.from(part);
    }

    /** Chặn đặt tình trạng Có sẵn khi phụ tùng chưa có ảnh nào. */
    private void requireReadyToSell(SparePart part, ProductStatus newStatus) {
        if (newStatus != ProductStatus.AVAILABLE) {
            return;
        }
        if (part.getPhotos().isEmpty()) {
            throw new ConflictException(
                    "Phụ tùng chưa có ảnh nên không đặt được tình trạng Có sẵn. "
                            + "Hãy lưu với tình trạng Hết hàng, thêm ảnh rồi chuyển sang Có sẵn.");
        }
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