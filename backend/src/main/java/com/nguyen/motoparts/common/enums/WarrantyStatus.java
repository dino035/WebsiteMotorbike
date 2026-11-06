package com.nguyen.motoparts.common.enums;

import lombok.Getter;

/** Trạng thái yêu cầu bảo hành (SRS 3.14, 3.22). */
@Getter
public enum WarrantyStatus {
    PENDING("Chờ xử lý"),
    APPROVED("Đã duyệt (thay mới)"),
    REJECTED("Từ chối");

    private final String label;

    WarrantyStatus(String label) {
        this.label = label;
    }
}
