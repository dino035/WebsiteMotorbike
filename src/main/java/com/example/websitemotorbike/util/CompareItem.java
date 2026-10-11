package com.example.websitemotorbike.util;

import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.VehicleConditionAssessment;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * Một cột trong bảng so sánh xe (SRS 3.8): xe, ảnh đại diện và các cờ "tốt nhất" để tô nổi giá trị đáng chú ý
 * (giá thấp nhất, đời mới nhất, ít chủ nhất, ít km nhất, điểm tình trạng cao nhất).
 * Một tiêu chí chỉ được tô khi các xe có giá trị khác nhau; nếu bằng nhau cả thì không tô.
 */
@Getter
public class CompareItem {

    private final Motorbike motorbike;
    private final String photoUrl;
    private boolean bestPrice;
    private boolean bestYear;
    private boolean bestOwners;
    private boolean bestOdometer;
    private boolean bestScore;

    public CompareItem(Motorbike motorbike, String photoUrl) {
        this.motorbike = motorbike;
        this.photoUrl = photoUrl;
    }

    /** Đánh dấu giá trị tốt nhất của từng tiêu chí trong danh sách các xe đang so sánh. */
    public static void markBest(List<CompareItem> items) {
        BigDecimal lowestPrice = best(items, i -> i.motorbike.getPrice(), false);
        Integer newestYear = best(items, i -> i.motorbike.getRegistrationYear(), true);
        Integer fewestOwners = best(items, i -> i.motorbike.getOwnerCount(), false);
        Integer lowestOdometer = best(items, i -> i.motorbike.getOdometerAtIntake(), false);
        Double highestScore = best(items, i -> i.score(), true);

        for (CompareItem item : items) {
            item.bestPrice = lowestPrice != null && item.motorbike.getPrice() != null
                    && lowestPrice.compareTo(item.motorbike.getPrice()) == 0;
            item.bestYear = newestYear != null && newestYear.equals(item.motorbike.getRegistrationYear());
            item.bestOwners = fewestOwners != null && fewestOwners.equals(item.motorbike.getOwnerCount());
            item.bestOdometer = lowestOdometer != null && lowestOdometer.equals(item.motorbike.getOdometerAtIntake());
            item.bestScore = highestScore != null && highestScore.equals(item.score());
        }
    }

    /** Điểm tình trạng /10, null nếu xe chưa được đánh giá. */
    private Double score() {
        VehicleConditionAssessment assessment = motorbike.getConditionAssessment();
        return assessment == null ? null : assessment.getAdjustmentCoefficient();
    }

    /**
     * Giá trị tốt nhất của một tiêu chí (lớn nhất nếu higherIsBetter, nhỏ nhất nếu ngược lại), hoặc null nếu
     * không có xe nào có giá trị hoặc mọi xe có giá trị đều bằng nhau (khi đó không có gì để tô).
     */
    private static <T extends Comparable<T>> T best(List<CompareItem> items, Function<CompareItem, T> getter,
                                                    boolean higherIsBetter) {
        T best = null;
        boolean allEqual = true;
        T first = null;
        int counted = 0;
        for (CompareItem item : items) {
            T value = getter.apply(item);
            if (value == null) {
                continue;
            }
            counted++;
            if (first == null) {
                first = value;
            } else if (first.compareTo(value) != 0) {
                allEqual = false;
            }
            if (best == null || (higherIsBetter ? value.compareTo(best) > 0 : value.compareTo(best) < 0)) {
                best = value;
            }
        }
        return counted < 2 || allEqual ? null : best;
    }
}
