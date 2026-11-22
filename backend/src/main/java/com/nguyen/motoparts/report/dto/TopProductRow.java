package com.nguyen.motoparts.report.dto;

import java.math.BigDecimal;

public record TopProductRow(String productId, String productName, long quantitySold, BigDecimal revenue) {
}
