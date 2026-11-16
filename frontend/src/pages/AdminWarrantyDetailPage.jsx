import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime } from '../utils/format.js'
import './OrderPages.css'
import './AdminPages.css'
import './WarrantyPages.css'

/** Admin: chi tiết một yêu cầu bảo hành; duyệt (thay mới) hoặc từ chối (bắt buộc có lý do) khi còn chờ xử lý. */
export default function AdminWarrantyDetailPage() {
    const { id } = useParams()
    const { logout } = useAuth()
    const [result, setResult] = useState({ id: null, detail: null, error: '' })
    const [note, setNote] = useState('')
    const [busy, setBusy] = useState(false)
    const [actionError, setActionError] = useState('')
    const [done, setDone] = useState('')

    useEffect(() => {
        let ignore = false
        client.get(`/admin/warranty-requests/${encodeURIComponent(id)}`)
            .then((res) => { if (!ignore) setResult({ id, detail: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) { logout(); return }
                const message = err.response?.status === 404 ? 'Không tìm thấy yêu cầu bảo hành này.'
                    : apiError(err, 'Không tải được yêu cầu bảo hành.')
                setResult({ id, detail: null, error: message })
            })
        return () => { ignore = true }
    }, [id, logout])

    async function decide(approve) {
        if (!approve && !note.trim()) {
            setActionError('Vui lòng nhập lý do từ chối.')
            return
        }
        setBusy(true)
        setActionError('')
        setDone('')
        try {
            const res = await client.post(`/admin/warranty-requests/${encodeURIComponent(id)}/decide`,
                { approve, note: note.trim() || null })
            setResult({ id, detail: res.data, error: '' })
            setDone(approve ? 'Đã duyệt yêu cầu bảo hành (thay mới).' : 'Đã từ chối yêu cầu bảo hành.')
        } catch (err) {
            if (err.response?.status === 401) logout()
            else setActionError(apiError(err))
        } finally {
            setBusy(false)
        }
    }

    const loading = result.id !== id
    const detail = result.detail
    const w = detail?.request

    return (
        <section className="admin">
            <h1>Quản trị</h1>
            <AdminNav />
            <Link to="/admin/warranty" className="order-detail__back">← Danh sách yêu cầu bảo hành</Link>

            {loading && <p className="admin__message">Đang tải...</p>}
            {!loading && result.error && <p className="admin__message admin__message--error">{result.error}</p>}

            {!loading && w && (
                <>
                    <div className="order-detail__head">
                        <h2 className="admin__title">Yêu cầu bảo hành #{w.shortCode}</h2>
                        <span className={`status-badge status-badge--${w.status.toLowerCase()}`}>{w.statusLabel}</span>
                    </div>
                    <p className="order-detail__date">Gửi lúc {formatDateTime(w.submittedAt)}</p>
                    {done && <p className="order-detail__success" role="status">{done}</p>}

                    <div className="order-detail__grid">
                        <div className="order-detail__box">
                            <h2>Nội dung yêu cầu</h2>
                            <p><strong>{w.productName}</strong> (đơn #{w.orderCode})</p>
                            <p className="warranty-card__text">{w.description}</p>
                            {w.attachmentUrl && (
                                <p className="warranty-card__sub">
                                    Minh chứng: <a href={w.attachmentUrl} target="_blank" rel="noopener noreferrer">{w.attachmentUrl}</a>
                                </p>
                            )}
                        </div>
                        <div className="order-detail__side">
                            <div className="order-detail__box">
                                <h2>Khách hàng</h2>
                                <p>{detail.customer.email}</p>
                                {detail.customer.phone && <p>{detail.customer.phone}</p>}
                            </div>
                            <div className="order-detail__box">
                                <h2>Hạn bảo hành</h2>
                                <p>Mua: {formatDateTime(detail.purchasedAt)}</p>
                                <p>Hạn: <strong>{formatDateTime(detail.deadline)}</strong></p>
                            </div>
                        </div>
                    </div>

                    {detail.canDecide ? (
                        <div className="order-detail__box warranty-decide">
                            <h2>Quyết định</h2>
                            <label>
                                Ghi chú gửi khách (bắt buộc khi từ chối)
                                <textarea rows="3" maxLength={500} value={note} onChange={(e) => setNote(e.target.value)}
                                          placeholder="Lý do từ chối, hoặc hướng dẫn nhận sản phẩm thay mới..." />
                            </label>
                            {actionError && <p className="admin__message admin__message--error" role="alert">{actionError}</p>}
                            <div className="warranty-form__actions">
                                <button type="button" className="warranty__btn warranty__btn--primary" disabled={busy}
                                        onClick={() => decide(true)}>Duyệt (thay mới)</button>
                                <button type="button" className="warranty__btn warranty__btn--danger" disabled={busy}
                                        onClick={() => decide(false)}>Từ chối</button>
                            </div>
                        </div>
                    ) : (
                        <div className="order-detail__box">
                            <h2>Kết quả</h2>
                            <p>
                                {w.status === 'REJECTED' ? 'Lý do từ chối' : 'Ghi chú'}
                                {w.decisionReason ? `: ${w.decisionReason}` : ': (không có)'}
                            </p>
                            {w.resolvedAt && <p className="warranty-card__sub">Xử lý lúc {formatDateTime(w.resolvedAt)}</p>}
                        </div>
                    )}
                </>
            )}
        </section>
    )
}
