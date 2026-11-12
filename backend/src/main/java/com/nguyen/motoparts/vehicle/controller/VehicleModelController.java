package com.nguyen.motoparts.vehicle.controller;

import com.nguyen.motoparts.vehicle.dto.VehicleYearOption;
import com.nguyen.motoparts.vehicle.service.VehicleModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle-models")
@RequiredArgsConstructor
public class VehicleModelController {

    private final VehicleModelService vehicleModelService;

    @GetMapping("/brands")
    public List<String> brands() {
        return vehicleModelService.getBrands();
    }

    @GetMapping("/models")
    public List<String> models(@RequestParam(name = "brand", required = false) String brand){
        return vehicleModelService.getModels(brand);
    }

    @GetMapping("/years")
    public List<VehicleYearOption> years(@RequestParam(name = "brand", required = false) String brand, @RequestParam(name = "model", required = false) String model){
        return vehicleModelService.getYears(brand, model);
    }
}
