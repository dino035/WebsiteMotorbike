package com.nguyen.motoparts.aftersales.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin duyệt (approve = true, thay mới) hoặc từ chối (approve = false, bắt buộc có note là lý do). */
public record WarrantyDecisionRequest(
        @NotNull(message = "Vui lòng chọn duyệt hoặc từ chối.")
        Boolean approve,

        @Size(max = 500, message = "Ghi chú quá dài (tối đa 500 ký tự).")
        String note) {
}
