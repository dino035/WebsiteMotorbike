package com.example.websitemotorbike.model;

import lombok.Getter;

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