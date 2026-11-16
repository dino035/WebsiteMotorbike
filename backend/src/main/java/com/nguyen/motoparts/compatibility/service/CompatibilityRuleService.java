package com.nguyen.motoparts.compatibility.service;

import com.nguyen.motoparts.catalog.dto.ProductSummaryResponse;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.repository.SparePartRepository;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.compatibility.dto.CreateRulesRequest;
import com.nguyen.motoparts.compatibility.dto.CreateRulesResult;
import com.nguyen.motoparts.compatibility.dto.RuleRow;
import com.nguyen.motoparts.compatibility.entity.CompatibilityRule;
import com.nguyen.motoparts.compatibility.repository.CompatibilityRuleRepository;
import com.nguyen.motoparts.vehicle.entity.VehicleModel;
import com.nguyen.motoparts.vehicle.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompatibilityRuleService {

    private final CompatibilityRuleRepository ruleRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final SparePartRepository sparePartRepository;

    @Transactional
    public CreateRulesResult createRules(CreateRulesRequest req, String adminAccountId) {
        // 1. Kiểm tra khoảng năm
        if (req.yearFrom() > req.yearTo()) {
            throw new BadRequestException("Năm bắt đầu phải nhỏ hơn hoặc bằng năm kết thúc.");
        }

        // 2. Tìm phụ tùng
        SparePart part = sparePartRepository.findById(req.sparePartId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phụ tùng."));

        // 3. Lấy các dòng xe (mỗi năm một dòng) trong khoảng năm
        List<VehicleModel> models = vehicleModelRepository
                .findByBrandAndModelAndYearFromBetweenOrderByYearFrom(
                        req.brand(), req.model(), req.yearFrom(), req.yearTo());

        // 4. Không có dòng xe nào thì báo lỗi rõ ràng
        if (models.isEmpty()) {
            throw new BadRequestException("Không tìm thấy dòng xe này trong khoảng năm đã chọn.");
        }

        // 5. Tạo luật cho từng năm, bỏ qua năm đã có luật
        int created = 0;
        int skipped = 0;
        for (VehicleModel vm : models) {
            if (ruleRepository.existsByVehicleModel_ModelIdAndSparePart_ProductId(
                    vm.getModelId(), part.getProductId())) {
                skipped++;
                continue;
            }
            CompatibilityRule rule = new CompatibilityRule();
            rule.setVehicleModel(vm);
            rule.setSparePart(part);
            rule.setUpdatedBy(adminAccountId);
            ruleRepository.save(rule);
            created++;
        }

        // 6. Trả kết quả
        return new CreateRulesResult(created, skipped);
    }

    @Transactional(readOnly = true)
    public List<RuleRow> listRules(String sparePartId) {
        if (!sparePartRepository.existsById(sparePartId)) {
            throw new ResourceNotFoundException("Không tìm thấy phụ tùng.");
        }
        return ruleRepository.findBySparePartWithVehicle(sparePartId)
                .stream()
                .map(RuleRow::from)
                .toList();
    }

    @Transactional
    public void deleteRule(String ruleId) {
        CompatibilityRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy luật tương thích."));
        ruleRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> findCompatibleParts(String modelId) {
        if (!vehicleModelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("Không tìm thấy dòng xe.");
        }
        return ruleRepository.findCompatibleParts(modelId)
                .stream()
                .map(ProductSummaryResponse::from)
                .toList();
    }
}