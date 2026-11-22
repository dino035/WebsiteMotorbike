package com.nguyen.motoparts.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(
        LocalDate from,
        LocalDate to,
        long orderCount,
        BigDecimal totalRevenue,
        List<DailyRevenueRow> daily,
        List<TopProductRow> topProducts,
        List<InventoryRow> inventory) {
}
