package com.nguyen.motoparts.catalog.dto;

import com.nguyen.motoparts.common.enums.RatingLevel;
import jakarta.validation.constraints.NotNull;

public record ConditionAssessmentRequest(
        @NotNull(message = "Vui lòng chấm điểm động cơ.")
        RatingLevel engineRating,

        @NotNull(message = "Vui lòng chấm điểm phanh.")
        RatingLevel brakeRating,

        @NotNull(message = "Vui lòng chấm điểm lốp.")
        RatingLevel tireRating,

        @NotNull(message = "Vui lòng chấm điểm truyền động.")
        RatingLevel chainRating,

        @NotNull(message = "Vui lòng chấm điểm hệ thống điện.")
        RatingLevel electricalRating) {
}