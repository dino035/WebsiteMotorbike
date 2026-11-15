package com.nguyen.motoparts.order.dto;

import com.nguyen.motoparts.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(
        @NotNull(message = "Vui lòng chọn trạng thái mới.")
        OrderStatus status) {
}
