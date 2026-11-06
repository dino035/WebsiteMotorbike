package com.nguyen.motoparts.common.enums;

import lombok.Getter;

/** Mức đánh giá một hạng mục tình trạng xe, kèm điểm 1–4 để tính điểm tình trạng. */
@Getter
public enum RatingLevel {
    POOR("Kém", 1),
    FAIR("Trung bình", 2),
    GOOD("Tốt", 3),
    EXCELLENT("Rất tốt", 4);

    private final String label;
    private final int score;

    RatingLevel(String label, int score) {
        this.label = label;
        this.score = score;
    }
}
