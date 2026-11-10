const priceFormat = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })

export const formatPrice = (value) => priceFormat.format(value ?? 0)

// "2026-11-10T22:30:00" -> "22:30" (giờ giữ xe hết hạn)
export function formatTime(localDateTime) {
    if (!localDateTime) return ''
    const d = new Date(localDateTime)
    if (Number.isNaN(d.getTime())) return ''
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}
