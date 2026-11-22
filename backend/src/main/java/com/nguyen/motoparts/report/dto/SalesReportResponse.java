package com.nguyen.motoparts.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(LocalDate from, LocalDate to, long orderCount, BigDecimal totalRevenue,
                                  BigDecimal averageOrderValue, List<DailyRevenueRow> daily,
                                  List<OrderStatusRow> ordersByStatus, List<TopProductRow> topProducts,
                                  List<InventoryRow> inventory) {}
