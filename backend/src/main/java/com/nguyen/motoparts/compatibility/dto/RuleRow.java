package com.nguyen.motoparts.compatibility.dto;

import com.nguyen.motoparts.compatibility.entity.CompatibilityRule;

public record RuleRow(String ruleId, String brand, String model, int year) {

    public static RuleRow from(CompatibilityRule r) {
        return new RuleRow(r.getRuleId(), r.getVehicleModel().getBrand(), r.getVehicleModel().getModel(), r.getVehicleModel().getYearFrom());
    }
}