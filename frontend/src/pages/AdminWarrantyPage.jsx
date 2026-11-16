import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime } from '../utils/format.js'
import { WARRANTY_STATUSES } from '../utils/orderStatus.js'
import './OrderPages.css'
import './AdminPages.css'
import './WarrantyPages.css'

/** Admin: các yêu cầu bảo hành, lọc theo trạng thái, phân trang. Bộ lọc và trang nằm trên địa chỉ nên tải lại vẫn giữ. */
export default function AdminWarrantyPage() {
    const { logout } = useAuth()
    const [params, setParams] = useSearchParams()
    const status = params.get('status') || ''
    const page = Math.max(Number.parseInt(params.get('page') || '0', 10) || 0, 0)

    const [result, setResult] = useState({ key: null, data: null, error: '' })
    const key = `${status}|${page}`

    useEffect(() => {
        let ignore = false
        client.get('/admin/warranty-requests', { params: { status: status || undefined, page, size: 15 } })
            .then((res) => { if (!ignore) setResult({ key, data: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setResult({ key, data: null, error: apiError(err, 'Không tải được danh sách yêu cầu bảo hành.') })
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
                {WARRANTY_STATUSES.map((s) => (
                    <button key={s.value} type="button"
                            className={status === s.value ? 'admin__filter admin__filter--on' : 'admin__filter'}
                            onClick={() => chooseStatus(s.value)}>{s.label}</button>
                ))}
            </div>

            {loading && <p className="admin__message">Đang tải...</p>}
            {!loading && result.error && <p className="admin__message admin__message--error">{result.error}</p>}

            {!loading && data && (
                data.items.length === 0 ? (
                    <p className="admin__message">Không có yêu cầu bảo hành nào.</p>
                ) : (
                    <>
                        <div className="admin__table-wrap">
                            <table className="admin__table">
                                <thead>
                                    <tr><th>Mã yêu cầu</th><th>Ngày gửi</th><th>Sản phẩm</th><th>Đơn</th><th>Khách</th><th>Trạng thái</th></tr>
                                </thead>
                                <tbody>
                                    {data.items.map((w) => (
                                        <tr key={w.requestId}>
                                            <td><Link to={`/admin/warranty/${w.requestId}`}><strong>#{w.shortCode}</strong></Link></td>
                                            <td>{formatDateTime(w.submittedAt)}</td>
                                            <td>{w.productName}</td>
                                            <td>#{w.orderCode}</td>
                                            <td>{w.customerEmail}</td>
                                            <td><span className={`status-badge status-badge--${w.status.toLowerCase()}`}>{w.statusLabel}</span></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <div className="admin__pager">
                            <button type="button" disabled={data.page <= 0} onClick={() => goToPage(data.page - 1)}>← Trước</button>
                            <span>Trang {data.page + 1} / {Math.max(data.totalPages, 1)} · {data.totalItems} yêu cầu</span>
                            <button type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => goToPage(data.page + 1)}>Sau →</button>
                        </div>
                    </>
                )
            )}
        </section>
    )
}
