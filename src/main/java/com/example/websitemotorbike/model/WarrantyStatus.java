package com.example.websitemotorbike.model;

import lombok.Getter;

/** Trạng thái yêu cầu bảo hành (SRS 3.14, 3.22). Không đổi tên hằng số vì CSDL lưu chuỗi này. */
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
