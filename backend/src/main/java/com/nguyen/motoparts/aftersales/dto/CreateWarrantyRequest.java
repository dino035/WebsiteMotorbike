package com.nguyen.motoparts.aftersales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Khách gửi yêu cầu bảo hành cho một sản phẩm đã mua. attachmentUrl (ảnh/video minh chứng) không bắt buộc. */
public record CreateWarrantyRequest(
        @NotBlank(message = "Vui lòng chọn sản phẩm cần bảo hành.")
        String orderItemId,

        @NotBlank(message = "Vui lòng mô tả lỗi.")
        @Size(max = 1000, message = "Mô tả lỗi quá dài (tối đa 1000 ký tự).")
        String description,

        @Size(max = 500, message = "Đường dẫn ảnh/video quá dài (tối đa 500 ký tự).")
        String attachmentUrl) {
}
