package com.nguyen.motoparts.advisory.dto;

import com.nguyen.motoparts.advisory.entity.CustomerVehicleProfile;
import com.nguyen.motoparts.vehicle.entity.VehicleModel;

import java.time.LocalDateTime;

/** Một hồ sơ xe. label là tên hiển thị sẵn: "Xe đi làm (Honda Wave Alpha 2015)", hoặc chỉ "Honda Wave Alpha 2015" khi không có biệt danh. */
public record VehicleProfileResponse(String profileId, String nickname, String vehicleModelId, String brand, String model,
                                     int registrationYear, String label, LocalDateTime createdAt) {

    /** Gọi trong giao dịch (dòng xe nạp lười). */
    public static VehicleProfileResponse from(CustomerVehicleProfile p) {
        VehicleModel v = p.getVehicleModel();
        String vehicle = v.getBrand() + " " + v.getModel() + " " + p.getRegistrationYear();
        String label = p.getNickname() == null ? vehicle : p.getNickname() + " (" + vehicle + ")";
        return new VehicleProfileResponse(p.getProfileId(), p.getNickname(), v.getModelId(), v.getBrand(), v.getModel(),
                p.getRegistrationYear(), label, p.getCreatedAt());
    }
}
