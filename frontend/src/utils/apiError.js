// Lấy câu báo lỗi tiếng Việt từ phản hồi của backend ({"message": "..."}); không có thì dùng câu dự phòng.
export function apiError(err, fallback = 'Có lỗi xảy ra, vui lòng thử lại.') {
    if (!err.response) return 'Không kết nối được máy chủ. Kiểm tra backend đã chạy chưa.'
    return err.response.data?.message || fallback
}
