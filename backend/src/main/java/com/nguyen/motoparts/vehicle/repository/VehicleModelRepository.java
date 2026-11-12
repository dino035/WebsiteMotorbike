package com.nguyen.motoparts.vehicle.repository;

import com.nguyen.motoparts.vehicle.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, String> {

    @Query("select distinct v.brand from VehicleModel v order by v.brand")
    List<String> findDistinctBrands();

    @Query("select distinct v.model from VehicleModel v where v.brand = :brand order by v.model")
    List<String> findDistinctModelsByBrand(@Param("brand") String brand);

    List<VehicleModel> findByBrandAndModelOrderByYearFromDesc(String brand, String model);
}
