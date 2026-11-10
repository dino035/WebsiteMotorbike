package com.nguyen.motoparts.common.enums;

import lombok.Getter;

/** Trạng thái lịch lắp đặt (SRS 3.11, 3.21). */
@Getter
public enum InstallationStatus {
    SCHEDULED("Đã đặt lịch"),
    DONE("Đã lắp đặt"),
    CANCELLED("Đã hủy");

    private final String label;

    InstallationStatus(String label) {
        this.label = label;
    }
}
