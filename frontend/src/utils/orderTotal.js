/**
 * Tổng của đơn có đang gồm phí công lắp đặt không, để bảng chi tiết chỉ liệt kê những dòng cộng ra đúng "Tổng cộng".
 * Không suy từ trạng thái lịch: hủy riêng lịch thì phí được trừ khỏi tổng, còn hủy cả đơn thì lịch cũng thành
 * CANCELLED nhưng tổng giữ nguyên.
 */
export function totalIncludesLaborFee(order) {
    if (!order.installation) return false
    const itemsTotal = order.items.reduce((sum, item) => sum + item.lineTotal, 0)
    return Math.round(order.totalAmount - itemsTotal) > 0
}
