package com.nguyen.motoparts.vehicle.dto;

import com.nguyen.motoparts.vehicle.entity.VehicleModel;

public record VehicleYearOption(String modelId, int year) {
    public static VehicleYearOption from(VehicleModel v) {
        return new VehicleYearOption(v.getModelId(), v.getYearFrom());
    }
}
