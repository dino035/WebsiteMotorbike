import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDate, formatPrice, isoDateFromToday } from '../utils/format.js'
import { INSTALLATION_STATUSES } from '../utils/orderStatus.js'
import './OrderPages.css'
import './AdminPages.css'

const MAX_DAYS_AHEAD = 60   // khớp InstallationPolicy.MAX_DAYS_AHEAD ở backend

/** Admin: lịch lắp đặt phụ tùng. Mỗi lịch đang "Đã đặt lịch" có thể đánh dấu đã lắp xong, hủy hoặc đổi ngày. */
export default function AdminInstallationsPage() {
    const { logout } = useAuth()
    const [status, setStatus] = useState('SCHEDULED')
    const [result, setResult] = useState({ status: null, rows: null, error: '' })
    const [busyId, setBusyId] = useState(null)
    const [actionError, setActionError] = useState('')
    const [rescheduling, setRescheduling] = useState(null)   // { requestId, date }

    useEffect(() => {
        let ignore = false
        client.get('/admin/installations', { params: { status: status || undefined } })
            .then((res) => { if (!ignore) setResult({ status, rows: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setResult({ status, rows: null, error: apiError(err, 'Không tải được lịch lắp đặt.') })
            })
        return () => { ignore = true }
    }, [status, logout])

    async function act(row, body, afterDone) {
        setBusyId(row.requestId)
        setActionError('')
        try {
            const res = await client.post(`/admin/installations/${encodeURIComponent(row.requestId)}/action`, body)
            // Cập nhật dòng ngay; nếu đang lọc theo "Đã đặt lịch" mà dòng đã đổi trạng thái thì dòng biến khỏi danh sách.
            setResult((r) => ({
                ...r,
                rows: r.rows
                    .map((x) => (x.requestId === row.requestId ? res.data : x))
                    .filter((x) => !status || x.status === status),
            }))
            afterDone?.()
        } catch (err) {
            if (err.response?.status === 401) logout()
            else setActionError(apiError(err))
        } finally {
            setBusyId(null)
        }
    }

    function cancel(row) {
        if (window.confirm(`Hủy lịch lắp đặt của đơn #${row.orderShortCode}?`)) act(row, { action: 'CANCEL' })
    }

    function reschedule(row) {
        if (!rescheduling?.date) {
            setActionError('Vui lòng chọn ngày mới.')
            return
        }
        act(row, { action: 'RESCHEDULE', newDate: rescheduling.date }, () => setRescheduling(null))
    }

    const loading = result.status !== status
    const rows = result.rows

    return (
        <section className="admin">
            <h1>Quản trị</h1>
            <AdminNav />

            <div className="admin__filters" role="tablist" aria-label="Lọc theo trạng thái">
                <button type="button" className={status === '' ? 'admin__filter admin__filter--on' : 'admin__filter'}
                        onClick={() => setStatus('')}>Tất cả</button>
                {INSTALLATION_STATUSES.map((s) => (
                    <button key={s.value} type="button"
                            className={status === s.value ? 'admin__filter admin__filter--on' : 'admin__filter'}
                            onClick={() => setStatus(s.value)}>{s.label}</button>
                ))}
            </div>

            {loading && <p className="admin__message">Đang tải...</p>}
            {!loading && result.error && <p className="admin__message admin__message--error">{result.error}</p>}
            {actionError && <p className="admin__message admin__message--error" role="alert">{actionError}</p>}

            {!loading && rows && (rows.length === 0 ? (
                <p className="admin__message">Không có lịch lắp đặt nào.</p>
            ) : (
                <div className="admin__table-wrap">
                    <table className="admin__table">
                        <thead>
                            <tr><th>Ngày hẹn</th><th>Đơn</th><th>Khách</th><th>Người nhận</th><th>Phí công</th><th>Lịch</th><th /></tr>
                        </thead>
                        <tbody>
                            {rows.map((r) => (
                                <tr key={r.requestId}>
                                    <td><strong>{formatDate(r.preferredDate)}</strong></td>
                                    <td>
                                        <Link to={`/admin/orders/${r.orderId}`}>#{r.orderShortCode}</Link>
                                        <span className={`status-badge status-badge--${r.orderStatus.toLowerCase()}`}>{r.orderStatusLabel}</span>
                                    </td>
                                    <td>{r.customerEmail}</td>
                                    <td>{r.receiverName}<span className="admin__sub">{r.receiverPhone}</span></td>
                                    <td>{formatPrice(r.laborFee)}</td>
                                    <td><span className={`status-badge status-badge--${installationClass(r.status)}`}>{r.statusLabel}</span></td>
                                    <td>
                                        {r.status === 'SCHEDULED' && (
                                            rescheduling?.requestId === r.requestId ? (
                                                <div className="admin__inline">
                                                    <input type="date" value={rescheduling.date}
                                                           min={isoDateFromToday(1)} max={isoDateFromToday(MAX_DAYS_AHEAD)}
                                                           onChange={(e) => setRescheduling({ requestId: r.requestId, date: e.target.value })} />
                                                    <button type="button" className="admin__btn admin__btn--primary" disabled={busyId === r.requestId}
                                                            onClick={() => reschedule(r)}>Lưu</button>
                                                    <button type="button" className="admin__btn" onClick={() => setRescheduling(null)}>Bỏ</button>
                                                </div>
                                            ) : (
                                                <div className="admin__inline">
                                                    <button type="button" className="admin__btn admin__btn--primary" disabled={busyId === r.requestId}
                                                            onClick={() => act(r, { action: 'DONE' })}>Đã lắp xong</button>
                                                    <button type="button" className="admin__btn" disabled={busyId === r.requestId}
                                                            onClick={() => { setRescheduling({ requestId: r.requestId, date: '' }); setActionError('') }}>Đổi ngày</button>
                                                    <button type="button" className="admin__btn admin__btn--danger" disabled={busyId === r.requestId}
                                                            onClick={() => cancel(r)}>Hủy lịch</button>
                                                </div>
                                            )
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            ))}
        </section>
    )
}

// Lịch dùng chung màu với trạng thái đơn: đã đặt = chờ (vàng), đã lắp = xong (xanh), hủy = xám.
function installationClass(status) {
    if (status === 'DONE') return 'completed'
    if (status === 'CANCELLED') return 'cancelled'
    return 'pending'
}
