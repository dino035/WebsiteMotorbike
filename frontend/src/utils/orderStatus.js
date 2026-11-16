// Trạng thái đơn hàng, khớp OrderStatus ở backend (thứ tự là quy trình xử lý đơn).
export const ORDER_STATUSES = [
    { value: 'PENDING', label: 'Chờ xác nhận' },
    { value: 'CONFIRMED', label: 'Đã xác nhận' },
    { value: 'SHIPPING', label: 'Đang giao/Chờ lấy' },
    { value: 'COMPLETED', label: 'Hoàn thành' },
    { value: 'CANCELLED', label: 'Đã hủy' },
]

// Trạng thái lịch lắp đặt, khớp InstallationStatus ở backend.
export const INSTALLATION_STATUSES = [
    { value: 'SCHEDULED', label: 'Đã đặt lịch' },
    { value: 'DONE', label: 'Đã lắp đặt' },
    { value: 'CANCELLED', label: 'Đã hủy' },
]

// Trạng thái yêu cầu bảo hành, khớp WarrantyStatus ở backend.
export const WARRANTY_STATUSES = [
    { value: 'PENDING', label: 'Chờ xử lý' },
    { value: 'APPROVED', label: 'Đã duyệt (thay mới)' },
    { value: 'REJECTED', label: 'Từ chối' },
]
