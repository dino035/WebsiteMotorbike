package com.nguyen.motoparts.vehicle.service;

import com.nguyen.motoparts.vehicle.dto.VehicleYearOption;
import com.nguyen.motoparts.vehicle.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gợi ý dòng xe theo kiểu chọn dần: hãng → dòng xe → năm (mỗi năm một VehicleModel). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleModelService {

    private final VehicleModelRepository vehicleModelRepository;

    public List<String> getBrands() {
        return vehicleModelRepository.findDistinctBrands();
    }

    public List<String> getModels(String brand) {
        if (brand == null || brand.isBlank()) {
            return List.of();
        }
        return vehicleModelRepository.findDistinctModelsByBrand(brand.trim());
    }

    public List<VehicleYearOption> getYears(String brand, String model) {
        if (brand == null || brand.isBlank() || model == null || model.isBlank()) {
            return List.of();
        }
        return vehicleModelRepository.findByBrandAndModelOrderByYearFromDesc(brand.trim(), model.trim())
                .stream()
                .map(VehicleYearOption::from)
                .toList();
    }
}