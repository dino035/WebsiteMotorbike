import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime, formatPrice } from '../utils/format.js'
import { ORDER_STATUSES } from '../utils/orderStatus.js'
import './OrderPages.css'
import './AdminPages.css'

/** Admin: mọi đơn hàng, lọc theo trạng thái, phân trang. Bộ lọc và trang nằm trên địa chỉ nên tải lại hay quay lại vẫn giữ. */
export default function AdminOrdersPage() {
    const { logout } = useAuth()
    const [params, setParams] = useSearchParams()
    const status = params.get('status') || ''
    const page = Math.max(Number.parseInt(params.get('page') || '0', 10) || 0, 0)

    const [result, setResult] = useState({ key: null, data: null, error: '' })
    const key = `${status}|${page}`

    useEffect(() => {
        let ignore = false
        client.get('/admin/orders', { params: { status: status || undefined, page, size: 15 } })
            .then((res) => { if (!ignore) setResult({ key, data: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setResult({ key, data: null, error: apiError(err, 'Không tải được danh sách đơn hàng.') })
            })
        return () => { ignore = true }
    }, [status, page, key, logout])

    function chooseStatus(value) {
        const next = new URLSearchParams()
        if (value) next.set('status', value)
        setParams(next)
    }

    function goToPage(p) {
        const next = new URLSearchParams(params)
        next.set('page', String(p))
        setParams(next)
    }

    const loading = result.key !== key
    const data = result.data

    return (
        <section className="admin">
            <h1>Quản trị</h1>
            <AdminNav />

            <div className="admin__filters" role="tablist" aria-label="Lọc theo trạng thái">
                <button type="button" className={status === '' ? 'admin__filter admin__filter--on' : 'admin__filter'}
                        onClick={() => chooseStatus('')}>Tất cả</button>
                {ORDER_STATUSES.map((s) => (
                    <button key={s.value} type="button"
                            className={status === s.value ? 'admin__filter admin__filter--on' : 'admin__filter'}
                            onClick={() => chooseStatus(s.value)}>{s.label}</button>
                ))}
            </div>

            {loading && <p className="admin__message">Đang tải...</p>}
            {!loading && result.error && <p className="admin__message admin__message--error">{result.error}</p>}

            {!loading && data && (
                data.items.length === 0 ? (
                    <p className="admin__message">Không có đơn hàng nào.</p>
                ) : (
                    <>
                        <div className="admin__table-wrap">
                            <table className="admin__table">
                                <thead>
                                    <tr>
                                        <th>Mã đơn</th><th>Ngày đặt</th><th>Khách</th><th>Nhận hàng</th>
                                        <th>Thanh toán</th><th>Tổng tiền</th><th>Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {data.items.map((o) => (
                                        <tr key={o.orderId}>
                                            <td><Link to={`/admin/orders/${o.orderId}`}><strong>#{o.shortCode}</strong></Link></td>
                                            <td>{formatDateTime(o.orderDate)}</td>
                                            <td>{o.customerEmail}<span className="admin__sub">{o.receiverName}</span></td>
                                            <td>{o.deliveryMethodLabel}</td>
                                            <td>
                                                {o.paymentMethod}
                                                <span className={`pay-badge pay-badge--${(o.paymentStatus || '').toLowerCase()}`}>{o.paymentStatusLabel}</span>
                                            </td>
                                            <td>{formatPrice(o.totalAmount)}</td>
                                            <td><span className={`status-badge status-badge--${o.status.toLowerCase()}`}>{o.statusLabel}</span></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <div className="admin__pager">
                            <button type="button" disabled={data.page <= 0} onClick={() => goToPage(data.page - 1)}>← Trước</button>
                            <span>Trang {data.page + 1} / {Math.max(data.totalPages, 1)} · {data.totalItems} đơn</span>
                            <button type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => goToPage(data.page + 1)}>Sau →</button>
                        </div>
                    </>
                )
            )}
        </section>
    )
}
