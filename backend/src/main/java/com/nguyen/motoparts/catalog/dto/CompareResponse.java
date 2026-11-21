package com.nguyen.motoparts.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Kết quả so sánh song song 2–4 xe máy (SRS 3.8).
 * Mỗi cờ best* cho biết xe này đang tốt nhất ở tiêu chí đó, để giao diện tô nổi.
 * Cờ chỉ bật khi các xe có giá trị khác nhau; bằng nhau cả thì không xe nào được tô.
 */
public record CompareResponse(List<CompareRow> items) {

    public record CompareRow(
            String productId,
            String name,
            BigDecimal price,
            String photoUrl,
            String brand,
            String model,
            Integer registrationYear,
            Integer ownerCount,
            Integer odometerAtIntake,
            String accidentHistory,
            Double conditionScore,
            String verdict,
            boolean bestPrice,
            boolean bestYear,
            boolean bestOwners,
            boolean bestOdometer,
            boolean bestScore) {
    }
}
