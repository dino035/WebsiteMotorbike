package com.nguyen.motoparts.compatibility.repository;

import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.compatibility.entity.CompatibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRule, String> {
    boolean existsByVehicleModel_ModelIdAndSparePart_ProductId(String modelId, String sparePartId);

    @Query("select r from CompatibilityRule r join fetch r.vehicleModel v " +
            "where r.sparePart.productId = :sparePartId order by v.brand, v.model, v.yearFrom")
    List<CompatibilityRule> findBySparePartWithVehicle(@Param("sparePartId") String sparePartId);

    @Query("select r.sparePart from CompatibilityRule r " +
            "where r.vehicleModel.modelId = :modelId order by r.sparePart.name")
    List<SparePart> findCompatibleParts(@Param("modelId") String modelId);
}
