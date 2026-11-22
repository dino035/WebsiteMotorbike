import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDate, formatPrice } from '../utils/format.js'
import './AdminPages.css'
import './OrderPages.css'   // .status-badge--* dùng cho bảng đơn theo trạng thái
import './AdminReports.css'

/**
 * Admin: báo cáo doanh thu (UC-23). Khoảng ngày nằm trên địa chỉ nên tải lại hay gửi link cho người khác vẫn ra
 * đúng báo cáo đó. Để trống thì máy chủ tự lấy 30 ngày gần nhất — không tính lại mặc định ở đây để chỉ có một
 * nơi quyết định, tránh hai bên lệch nhau.
 */
export default function AdminReportsPage() {
    const { logout } = useAuth()
    const [params, setParams] = useSearchParams()
    const from = params.get('from') || ''
    const to = params.get('to') || ''

    const [fromInput, setFromInput] = useState(from)
    const [toInput, setToInput] = useState(to)
    const [result, setResult] = useState({ key: null, data: null, error: '' })
    const key = `${from}|${to}`

    useEffect(() => {
        let ignore = false
        client.get('/admin/reports/sales', { params: { from: from || undefined, to: to || undefined } })
            .then((res) => { if (!ignore) setResult({ key, data: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setResult({ key, data: null, error: apiError(err, 'Không tải được báo cáo.') })
            })
        return () => { ignore = true }
    }, [from, to, key, logout])

    function apply(event) {
        event.preventDefault()
        const next = new URLSearchParams()
        if (fromInput) next.set('from', fromInput)
        if (toInput) next.set('to', toInput)
        setParams(next)
    }

    function reset() {
        setFromInput('')
        setToInput('')
        setParams(new URLSearchParams())
    }

    const loading = result.key !== key
    const data = result.data
    const maxRevenue = data ? Math.max(...data.daily.map((d) => Number(d.revenue)), 0) : 0

    return (
        <section className="admin">
            <h1>Quản trị</h1>
            <AdminNav />

            <form className="admin__actions" onSubmit={apply}>
                <span className="admin__inline">
                    <label htmlFor="report-from">Từ ngày</label>
                    <input id="report-from" type="date" value={fromInput}
                           onChange={(e) => setFromInput(e.target.value)} />
                </span>
                <span className="admin__inline">
                    <label htmlFor="report-to">Đến ngày</label>
                    <input id="report-to" type="date" value={toInput}
                           onChange={(e) => setToInput(e.target.value)} />
                </span>
                <button type="submit" className="admin__btn admin__btn--primary">Xem</button>
                <button type="button" className="admin__btn" onClick={reset}>30 ngày gần nhất</button>
            </form>

            {loading && <p className="admin__message">Đang tải...</p>}
            {!loading && result.error && <p className="admin__message admin__message--error">{result.error}</p>}

            {!loading && data && (
                <>
                    <div className="report__cards">
                        <div className="report__card">
                            <span className="report__card-label">Khoảng xem</span>
                            <strong>{formatDate(data.from)} – {formatDate(data.to)}</strong>
                        </div>
                        <div className="report__card">
                            <span className="report__card-label">Đơn hoàn thành</span>
                            <strong>{data.orderCount}</strong>
                        </div>
                        <div className="report__card">
                            <span className="report__card-label">Doanh thu</span>
                            <strong>{formatPrice(data.totalRevenue)}</strong>
                        </div>
                        <div className="report__card">
                            <span className="report__card-label">Trung bình mỗi đơn</span>
                            <strong>{formatPrice(data.averageOrderValue)}</strong>
                        </div>
                    </div>

                    <h2 className="report__heading">Doanh thu theo ngày</h2>
                    {maxRevenue === 0 ? (
                        <p className="admin__message">Không có đơn hoàn thành nào trong khoảng này.</p>
                    ) : (
                        <div className="report__chart-wrap">
                            <ul className="report__chart">
                                {data.daily.map((d) => (
                                    <li key={d.date} className="report__bar-slot"
                                        title={`${formatDate(d.date)} · ${d.orderCount} đơn · ${formatPrice(d.revenue)}`}>
                                        <span className="report__bar"
                                              style={{ height: `${(Number(d.revenue) / maxRevenue) * 100}%` }} />
                                    </li>
                                ))}
                            </ul>
                            <p className="report__axis">
                                <span>{formatDate(data.from)}</span>
                                <span>Cao nhất: {formatPrice(maxRevenue)}</span>
                                <span>{formatDate(data.to)}</span>
                            </p>
                        </div>
                    )}

                    <h2 className="report__heading">Đơn hàng theo trạng thái</h2>
                    <div className="admin__table-wrap">
                        <table className="admin__table">
                            <thead>
                            <tr><th>Trạng thái</th><th>Số đơn</th></tr>
                            </thead>
                            <tbody>
                            {data.ordersByStatus.map((row) => (
                                <tr key={row.status}>
                                    <td>
                                            <span className={`status-badge status-badge--${row.status.toLowerCase()}`}>
                                                {row.statusLabel}
                                            </span>
                                    </td>
                                    <td>{row.count}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>

                    <h2 className="report__heading">Sản phẩm bán chạy</h2>
                    {data.topProducts.length === 0 ? (
                        <p className="admin__message">Chưa có sản phẩm nào bán được trong khoảng này.</p>
                    ) : (
                        <div className="admin__table-wrap">
                            <table className="admin__table">
                                <thead>
                                <tr><th>#</th><th>Sản phẩm</th><th>Số lượng</th><th>Doanh thu</th></tr>
                                </thead>
                                <tbody>
                                {data.topProducts.map((p, i) => (
                                    <tr key={p.productId}>
                                        <td>{i + 1}</td>
                                        <td>{p.productName}</td>
                                        <td>{p.quantitySold}</td>
                                        <td>{formatPrice(p.revenue)}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    <h2 className="report__heading">Sản phẩm theo trạng thái</h2>
                    <p className="admin__hint">Số liệu tại thời điểm xem, không phụ thuộc khoảng ngày.</p>
                    <div className="admin__table-wrap">
                        <table className="admin__table">
                            <thead>
                            <tr><th>Loại</th><th>Trạng thái</th><th>Số lượng</th><th>Giá trị</th></tr>
                            </thead>
                            <tbody>
                            {data.inventory.map((row) => (
                                <tr key={`${row.typeLabel}|${row.statusLabel}`}>
                                    <td>{row.typeLabel}</td>
                                    <td>{row.statusLabel}</td>
                                    <td>{row.count}</td>
                                    <td>{formatPrice(row.totalValue)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}
        </section>
    )
}
