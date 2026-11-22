package com.nguyen.motoparts.report.dto;

import java.math.BigDecimal;

public record InventoryRow(String typeLabel, String statusLabel, long count, BigDecimal totalValue) {}
