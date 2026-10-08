package com.example.websitemotorbike.model;

import lombok.Getter;

/** Trạng thái lịch lắp đặt tại cửa hàng (SRS 3.11, 3.21). Không đổi tên hằng số vì CSDL lưu chuỗi này. */
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
