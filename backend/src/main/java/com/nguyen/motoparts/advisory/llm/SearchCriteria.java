package com.nguyen.motoparts.advisory.llm;

import com.nguyen.motoparts.common.enums.PartOriginType;

/**
 * Tiêu chí tìm sản phẩm mà LLM đọc ra từ câu hỏi của khách. Mọi trường (trừ intent, kind) có thể null = không lọc.
 * Dữ liệu từ LLM không đáng tin hoàn toàn, nên {@link #sanitized()} luôn được gọi trước khi dùng để lọc CSDL.
 */
public record SearchCriteria(
        Intent intent,
        Kind kind,
        String keyword,
        String brand,
        String model,
        Integer vehicleYear,
        Long minPrice,
        Long maxPrice,
        String partType,
        PartOriginType partOrigin,
        Integer minRegistrationYear,
        Integer maxRegistrationYear) {

    /** SEARCH: muốn tìm sản phẩm; GREETING: chào hỏi; OTHER: ngoài phạm vi (không liên quan xe, phụ tùng). */
    public enum Intent { SEARCH, GREETING, OTHER }

    /** MOTORBIKE: xe máy cũ; SPARE_PART: phụ tùng; ANY: chưa rõ. */
    public enum Kind { MOTORBIKE, SPARE_PART, ANY }

    private static final int MAX_TEXT = 60;
    private static final long MAX_PRICE = 1_000_000_000_000L;

    public static SearchCriteria other() {
        return new SearchCriteria(Intent.OTHER, Kind.ANY, null, null, null, null, null, null, null, null, null, null);
    }

    /** Làm sạch: chuỗi cắt gọn, năm và giá nằm trong khoảng hợp lý, khoảng đảo ngược thì đổi chỗ. */
    public SearchCriteria sanitized() {
        Long min = price(minPrice);
        Long max = price(maxPrice);
        if (min != null && max != null && min > max) {
            Long t = min;
            min = max;
            max = t;
        }
        Integer yFrom = year(minRegistrationYear);
        Integer yTo = year(maxRegistrationYear);
        if (yFrom != null && yTo != null && yFrom > yTo) {
            Integer t = yFrom;
            yFrom = yTo;
            yTo = t;
        }
        return new SearchCriteria(
                intent == null ? Intent.OTHER : intent,
                kind == null ? Kind.ANY : kind,
                text(keyword), text(brand), text(model), year(vehicleYear), min, max,
                text(partType), partOrigin, yFrom, yTo);
    }

    private static String text(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > MAX_TEXT ? t.substring(0, MAX_TEXT) : t;
    }

    private static Integer year(Integer y) {
        return (y == null || y < 1950 || y > 2100) ? null : y;
    }

    private static Long price(Long p) {
        return (p == null || p < 0 || p > MAX_PRICE) ? null : p;
    }
}
