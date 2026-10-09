package com.example.websitemotorbike.util;

import com.example.websitemotorbike.model.RatingLevel;
import com.example.websitemotorbike.model.VehicleConditionAssessment;
import jakarta.servlet.http.HttpServletRequest;

public class ConditionInput {

    /** Đọc 5 mức đánh giá từ form vào assessment và tính lại hệ số. Ném IllegalArgumentException nếu thiếu hoặc sai. */
    public static void readInto(HttpServletRequest request, VehicleConditionAssessment assessment) {
        assessment.setEngineRating(read(request, "engineRating", "động cơ"));
        assessment.setBrakeRating(read(request, "brakeRating", "phanh"));
        assessment.setTireRating(read(request, "tireRating", "lốp"));
        assessment.setChainRating(read(request, "chainRating", "truyền động"));
        assessment.setElectricalRating(read(request, "electricalRating", "hệ thống điện"));
        assessment.recalculateCoefficient();
    }

    private static RatingLevel read(HttpServletRequest request, String field, String label) {
        String value = request.getParameter(field);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn mức đánh giá " + label);
        }
        try {
            return RatingLevel.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Mức đánh giá " + label + " không hợp lệ");
        }
    }
}