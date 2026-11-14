const priceFormat = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })

export const formatPrice = (value) => priceFormat.format(value ?? 0)

// "2026-11-10T22:30:00" -> "22:30" (giờ giữ xe hết hạn)
export function formatTime(localDateTime) {
    if (!localDateTime) return ''
    const d = new Date(localDateTime)
    if (Number.isNaN(d.getTime())) return ''
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

// "2026-11-10T22:30:00" -> "10/11/2026 22:30"
export function formatDateTime(localDateTime) {
    if (!localDateTime) return ''
    const d = new Date(localDateTime)
    if (Number.isNaN(d.getTime())) return ''
    return d.toLocaleString('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    })
}

// "2026-12-05" -> "05/12/2026" (ngày thuần, không đổi múi giờ)
export function formatDate(isoDate) {
    if (!isoDate) return ''
    const [y, m, d] = isoDate.split('-')
    return `${d}/${m}/${y}`
}

// Ngày dạng yyyy-MM-dd theo giờ máy khách, cộng thêm số ngày (dùng cho ô chọn ngày hẹn lắp đặt).
export function isoDateFromToday(plusDays) {
    const d = new Date()
    d.setDate(d.getDate() + plusDays)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `${d.getFullYear()}-${mm}-${dd}`
}
