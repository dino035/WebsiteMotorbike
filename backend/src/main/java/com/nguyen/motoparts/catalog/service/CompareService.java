package com.nguyen.motoparts.catalog.service;

import com.nguyen.motoparts.catalog.dto.CompareResponse;
import com.nguyen.motoparts.catalog.dto.CompareResponse.CompareRow;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.repository.MotorbikeRepository;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.vehicle.entity.VehicleConditionAssessment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** So sánh song song 2–4 xe máy (SRS 3.8). Trang công khai, không cần đăng nhập. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompareService {

    public static final int MIN_COMPARE = 2;
    public static final int MAX_COMPARE = 4;

    private final MotorbikeRepository motorbikeRepository;

    public CompareResponse compare(List<String> ids) {
        // Bỏ mã trống và mã trùng, giữ nguyên thứ tự người dùng chọn
        List<String> unique = (ids == null ? List.<String>of() : ids).stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (unique.size() < MIN_COMPARE) {
            throw new BadRequestException("Vui lòng chọn ít nhất " + MIN_COMPARE + " xe để so sánh.");
        }
        if (unique.size() > MAX_COMPARE) {
            throw new BadRequestException(
                    "Chỉ so sánh được tối đa " + MAX_COMPARE + " xe, vui lòng bỏ bớt lựa chọn.");
        }

        List<Motorbike> found = motorbikeRepository.findAllById(unique);
        if (found.size() != unique.size()) {
            throw new ResourceNotFoundException("Có mã không tồn tại hoặc không phải xe máy, vui lòng chọn lại.");
        }

        List<String> notAvailable = found.stream()
                .filter(bike -> bike.getStatus() != ProductStatus.AVAILABLE)
                .map(Motorbike::getName)
                .toList();
        if (!notAvailable.isEmpty()) {
            throw new BadRequestException(
                    "Chỉ so sánh được xe đang bán. Xe không còn bán: " + String.join(", ", notAvailable));
        }

        Map<String, Motorbike> byId = found.stream()
                .collect(Collectors.toMap(Motorbike::getProductId, bike -> bike));
        List<Motorbike> bikes = unique.stream().map(byId::get).toList();

        BigDecimal bestPrice = best(bikes, Motorbike::getPrice, false);
        Integer bestYear = best(bikes, Motorbike::getRegistrationYear, true);
        Integer bestOwners = best(bikes, Motorbike::getOwnerCount, false);
        Integer bestOdometer = best(bikes, Motorbike::getOdometerAtIntake, false);
        Double bestScore = best(bikes, CompareService::score, true);

        List<CompareRow> rows = bikes.stream()
                .map(bike -> new CompareRow(
                        bike.getProductId(),
                        bike.getName(),
                        bike.getPrice(),
                        firstPhotoUrl(bike),
                        bike.getBrand(),
                        bike.getModel(),
                        bike.getRegistrationYear(),
                        bike.getOwnerCount(),
                        bike.getOdometerAtIntake(),
                        bike.getAccidentHistory(),
                        score(bike),
                        verdict(bike),
                        samePrice(bestPrice, bike.getPrice()),
                        Objects.equals(bestYear, bike.getRegistrationYear()),
                        Objects.equals(bestOwners, bike.getOwnerCount()),
                        Objects.equals(bestOdometer, bike.getOdometerAtIntake()),
                        Objects.equals(bestScore, score(bike))))
                .toList();

        return new CompareResponse(rows);
    }

    private static String firstPhotoUrl(Motorbike bike) {
        return bike.getPhotos().isEmpty() ? null : bike.getPhotos().get(0).getImageUrl();
    }

    /** Điểm tình trạng trên thang 10, null nếu xe chưa được chấm điểm. */
    private static Double score(Motorbike bike) {
        VehicleConditionAssessment assessment = bike.getConditionAssessment();
        return assessment == null ? null : assessment.getAdjustmentCoefficient();
    }

    private static String verdict(Motorbike bike) {
        VehicleConditionAssessment assessment = bike.getConditionAssessment();
        return assessment == null ? null : assessment.getVerdict();
    }

    /** BigDecimal so sánh bằng compareTo, vì equals còn xét cả số chữ số thập phân (10 khác 10.0). */
    private static boolean samePrice(BigDecimal best, BigDecimal value) {
        return best != null && value != null && best.compareTo(value) == 0;
    }

    /**
     * Giá trị tốt nhất của một tiêu chí: lớn nhất nếu higherIsBetter, nhỏ nhất nếu ngược lại.
     * Trả null khi có dưới 2 xe cho giá trị, hoặc mọi xe đều bằng nhau — khi đó không tô nổi gì cả.
     */
    private static <T extends Comparable<T>> T best(List<Motorbike> bikes,
                                                    Function<Motorbike, T> getter,
                                                    boolean higherIsBetter) {
        T best = null;
        T first = null;
        boolean allEqual = true;
        int counted = 0;

        for (Motorbike bike : bikes) {
            T value = getter.apply(bike);
            if (value == null) {
                continue;
            }
            counted++;
            if (first == null) {
                first = value;
            } else if (first.compareTo(value) != 0) {
                allEqual = false;
            }
            if (best == null || (higherIsBetter ? value.compareTo(best) > 0 : value.compareTo(best) < 0)) {
                best = value;
            }
        }

        return (counted < MIN_COMPARE || allEqual) ? null : best;
    }
}
