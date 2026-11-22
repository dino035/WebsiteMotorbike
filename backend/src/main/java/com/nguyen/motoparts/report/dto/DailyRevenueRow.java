package com.nguyen.motoparts.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueRow(LocalDate date, long orderCount, BigDecimal revenue) {
}

