package com.nguyen.motoparts.catalog.service;

import com.nguyen.motoparts.catalog.dto.ConditionAssessmentRequest;
import com.nguyen.motoparts.catalog.dto.MotorbikeRequest;
import com.nguyen.motoparts.catalog.dto.ProductDetailResponse;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.repository.MotorbikeRepository;
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

        String storagePath = photoStorage.upload(file, "motorbikes");

        VehiclePhoto photo = new VehiclePhoto();
        photo.setStoragePath(storagePath);
        photo.setImageUrl(photoStorage.publicUrl(storagePath));
        photo.setCaption(caption);
        bike.addPhoto(photo);
        motorbikeRepository.flush();

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

        bike.removePhoto(photo);
        photoStorage.delete(photo.getStoragePath());

        return ProductDetailResponse.from(bike);
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
