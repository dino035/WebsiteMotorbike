package com.nguyen.motoparts.order.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Admin xử lý một lịch đang SCHEDULED: DONE (đã lắp xong), CANCEL (hủy lịch) hoặc RESCHEDULE (đổi ngày, cần newDate). */
public record InstallationActionRequest(
        @NotNull(message = "Vui lòng chọn thao tác.")
        Action action,
        LocalDate newDate) {

    public enum Action {
        DONE, CANCEL, RESCHEDULE
    }
}
