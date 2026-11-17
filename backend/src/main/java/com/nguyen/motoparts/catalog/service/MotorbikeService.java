package com.nguyen.motoparts.catalog.service;

import com.nguyen.motoparts.catalog.dto.ConditionAssessmentRequest;
import com.nguyen.motoparts.catalog.dto.MotorbikeRequest;
import com.nguyen.motoparts.catalog.dto.ProductDetailResponse;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.repository.MotorbikeRepository;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.common.storage.PhotoStorageService;
import com.nguyen.motoparts.order.repository.OrderItemRepository;
import com.nguyen.motoparts.vehicle.entity.VehicleConditionAssessment;
import com.nguyen.motoparts.vehicle.entity.VehiclePhoto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MotorbikeService {

    private final MotorbikeRepository motorbikeRepository;
    private final OrderItemRepository orderItemRepository;
    private final PhotoStorageService photoStorage;

    @Transactional
    public ProductDetailResponse create(MotorbikeRequest req) {
        if (motorbikeRepository.existsByChassisNumber(req.chassisNumber())) {
            throw new ConflictException("Số khung này đã có xe khác sử dụng.");
        }
        if (motorbikeRepository.existsByEngineNumber(req.engineNumber())) {
            throw new ConflictException("Số máy này đã có xe khác sử dụng.");
        }

        Motorbike bike = new Motorbike();
        requireReadyToSell(bike, req.status());
        apply(req, bike);
        return ProductDetailResponse.from(motorbikeRepository.save(bike));
    }

    @Transactional
    public ProductDetailResponse update(String productId, MotorbikeRequest req) {
        Motorbike bike = motorbikeRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe."));

        if (motorbikeRepository.existsByChassisNumberAndProductIdNot(req.chassisNumber(), productId)) {
            throw new ConflictException("Số khung này đã có xe khác sử dụng.");
        }
        if (motorbikeRepository.existsByEngineNumberAndProductIdNot(req.engineNumber(), productId)) {
            throw new ConflictException("Số máy này đã có xe khác sử dụng.");
        }

        requireReadyToSell(bike, req.status());
        apply(req, bike);
        return ProductDetailResponse.from(bike);
    }

    @Transactional
    public void delete(String productId) {
        Motorbike bike = motorbikeRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe."));

        if (orderItemRepository.existsByProductId(productId)) {
            throw new ConflictException(
                    "Xe này đã có trong đơn hàng nên không xóa được. "
                            + "Bạn có thể đổi tình trạng sang Hết hàng.");
        }

        bike.getPhotos().forEach(p -> photoStorage.delete(p.getStoragePath()));
        motorbikeRepository.delete(bike);
    }

    @Transactional
    public ProductDetailResponse saveAssessment(String productId, ConditionAssessmentRequest req) {
        Motorbike bike = motorbikeRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe."));

        VehicleConditionAssessment assessment = bike.getConditionAssessment();
        if (assessment == null) {
            assessment = new VehicleConditionAssessment();
            bike.setConditionAssessment(assessment);
        }

        assessment.setEngineRating(req.engineRating());
        assessment.setBrakeRating(req.brakeRating());
        assessment.setTireRating(req.tireRating());
        assessment.setChainRating(req.chainRating());
        assessment.setElectricalRating(req.electricalRating());
        assessment.recalculateCoefficient();

        return ProductDetailResponse.from(bike);
    }

    @Transactional
    public ProductDetailResponse addPhoto(String productId, MultipartFile file, String caption) {
        Motorbike bike = motorbikeRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe."));

        if (bike.getPhotos().size() >= PhotoStorageService.MAX_PHOTOS) {
            throw new ConflictException(
                    "Mỗi xe chỉ được tối đa " + PhotoStorageService.MAX_PHOTOS
                            + " ảnh. Hãy xóa bớt ảnh cũ trước khi thêm ảnh mới.");
        }

        String storagePath = photoStorage.upload(file, "motorbikes");
        try {
            VehiclePhoto photo = new VehiclePhoto();
            photo.setStoragePath(storagePath);
            photo.setImageUrl(photoStorage.publicUrl(storagePath));
            photo.setCaption(caption);
            bike.addPhoto(photo);
            motorbikeRepository.flush();
        } catch (RuntimeException e) {
            photoStorage.delete(storagePath);   // lưu DB hỏng thì xóa luôn file vừa tải lên
            throw e;
        }

        return ProductDetailResponse.from(bike);
    }

    @Transactional
    public ProductDetailResponse deletePhoto(String productId, String photoId) {
        Motorbike bike = motorbikeRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy xe."));

        VehiclePhoto photo = bike.getPhotos().stream()
                .filter(p -> p.getPhotoId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh của xe này."));

        if (bike.getStatus() == ProductStatus.AVAILABLE && bike.getPhotos().size() <= 1) {
            throw new ConflictException(
                    "Xe đang bán phải có ít nhất 1 ảnh. "
                            + "Hãy thêm ảnh khác, hoặc đổi tình trạng sang Hết hàng trước khi xóa.");
        }

        bike.removePhoto(photo);
        photoStorage.delete(photo.getStoragePath());

        return ProductDetailResponse.from(bike);
    }

    /**
     * Chặn đặt tình trạng Có sẵn khi xe chưa đủ điều kiện bày bán:
     * phải có ít nhất 1 ảnh và phải được chấm điểm tình trạng.
     */
    private void requireReadyToSell(Motorbike bike, ProductStatus newStatus) {
        if (newStatus != ProductStatus.AVAILABLE) {
            return;
        }
        if (bike.getPhotos().isEmpty()) {
            throw new ConflictException(
                    "Xe chưa có ảnh nên không đặt được tình trạng Có sẵn. "
                            + "Hãy lưu với tình trạng Hết hàng, thêm ảnh rồi chuyển sang Có sẵn.");
        }
        if (bike.getConditionAssessment() == null) {
            throw new ConflictException(
                    "Xe chưa được chấm điểm tình trạng nên không đặt được tình trạng Có sẵn. "
                            + "Hãy lưu với tình trạng Hết hàng, chấm điểm rồi chuyển sang Có sẵn.");
        }
    }

    /** Chép dữ liệu từ request sang entity. Dùng chung cho thêm mới và sửa. */
    private void apply(MotorbikeRequest req, Motorbike bike) {
        bike.setName(req.name());
        bike.setPrice(req.price());
        bike.setStatus(req.status());
        bike.setBrand(req.brand());
        bike.setModel(req.model());
        bike.setRegistrationYear(req.registrationYear());
        bike.setOwnerCount(req.ownerCount());
        bike.setAccidentHistory(req.accidentHistory());
        bike.setChassisNumber(req.chassisNumber());
        bike.setEngineNumber(req.engineNumber());
        bike.setOdometerAtIntake(req.odometerAtIntake());
    }
}