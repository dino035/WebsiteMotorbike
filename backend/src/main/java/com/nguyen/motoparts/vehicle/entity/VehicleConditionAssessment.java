package com.nguyen.motoparts.vehicle.entity;

import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.common.enums.RatingLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Đánh giá tình trạng xe (đặc tả: 5 mức đánh giá + adjustmentCoefficient; Composition 1 — 1 từ Motorbike).
 * Như WebsiteMotorbike: adjustmentCoefficient là điểm tình trạng trên thang 10, tự tính từ 5 mức,
 * chỉ để hiển thị, không dùng để chỉnh giá.
 */
@Entity
@Getter
@Setter
public class VehicleConditionAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String assessmentId;

    @Enumerated(EnumType.STRING)
    private RatingLevel engineRating;

    @Enumerated(EnumType.STRING)
    private RatingLevel brakeRating;

    @Enumerated(EnumType.STRING)
    private RatingLevel tireRating;

    @Enumerated(EnumType.STRING)
    private RatingLevel chainRating;

    @Enumerated(EnumType.STRING)
    private RatingLevel electricalRating;

    /** Điểm tình trạng trên thang 10 (một chữ số thập phân). */
    private Double adjustmentCoefficient;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorbike_id", unique = true)
    private Motorbike motorbike;

    /** Đã đặt đủ 5 mức chưa (tránh NullPointerException khi tính điểm). */
    public boolean isComplete() {
        return engineRating != null && brakeRating != null && tireRating != null
                && chainRating != null && electricalRating != null;
    }

    /** Điểm trung bình 5 mục trên thang 1–4. */
    private double average() {
        int sum = engineRating.getScore() + brakeRating.getScore() + tireRating.getScore()
                + chainRating.getScore() + electricalRating.getScore();
        return sum / 5.0;
    }

    /** Điểm /10 = trung bình / 4 * 10, làm tròn 1 chữ số. Gọi sau khi đã đặt đủ 5 mức. */
    public void recalculateCoefficient() {
        this.adjustmentCoefficient = isComplete() ? Math.round(average() / 4.0 * 100) / 10.0 : null;
    }

    /** Kết luận chung cho khách xem, theo điểm trung bình 1–4. */
    public String getVerdict() {
        if (!isComplete()) {
            return "";
        }
        double avg = average();
        if (avg >= 3.5) {
            return "Tình trạng rất tốt, gần như không cần sửa chữa";
        }
        if (avg >= 2.5) {
            return "Tình trạng tốt, có thể cần bảo dưỡng nhẹ";
        }
        if (avg >= 1.5) {
            return "Tình trạng trung bình, nên kiểm tra và sửa một số hạng mục";
        }
        return "Tình trạng kém, cần sửa chữa nhiều";
    }

    /** Gợi ý theo từng hạng mục: chỉ liệt kê mục ở mức Kém hoặc Trung bình. */
    public List<String> getRepairAdvice() {
        List<String> advice = new ArrayList<>();
        addAdvice(advice, "Động cơ", engineRating);
        addAdvice(advice, "Phanh", brakeRating);
        addAdvice(advice, "Lốp", tireRating);
        addAdvice(advice, "Truyền động", chainRating);
        addAdvice(advice, "Hệ thống điện", electricalRating);
        return advice;
    }

    private void addAdvice(List<String> advice, String part, RatingLevel level) {
        if (level == RatingLevel.POOR) {
            advice.add(part + ": nên sửa hoặc thay");
        } else if (level == RatingLevel.FAIR) {
            advice.add(part + ": nên kiểm tra");
        }
    }
}
