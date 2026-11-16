import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client.js'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime } from '../utils/format.js'
import './OrderPages.css'
import './WarrantyPages.css'

const fetchAll = () => Promise.all([client.get('/warranty-requests/options'), client.get('/warranty-requests')])
    .then(([options, requests]) => ({ options: options.data, requests: requests.data }))

/**
 * Bảo hành của khách: chọn sản phẩm đã mua (đơn Hoàn thành) để gửi yêu cầu, và xem các yêu cầu đã gửi.
 * Xe bảo hành 2 năm, phụ tùng 6 tháng kể từ ngày mua; mỗi sản phẩm chỉ có một yêu cầu đang chờ xử lý.
 */
export default function WarrantyPage() {
    const { logout } = useAuth()
    const [data, setData] = useState(null)   // { options, requests }
    const [error, setError] = useState('')
    const [selected, setSelected] = useState(null)   // orderItemId đang soạn yêu cầu
    const [notice, setNotice] = useState('')

    const handleLoadError = useCallback((err) => {
        if (err.response?.status === 401) logout()
        else setError(apiError(err, 'Không tải được trang bảo hành.'))
    }, [logout])

    useEffect(() => {
        let ignore = false
        fetchAll()
            .then((result) => { if (!ignore) setData(result) })
            .catch((err) => { if (!ignore) handleLoadError(err) })
        return () => { ignore = true }
    }, [handleLoadError])

    async function submitted(created) {
        setSelected(null)
        setNotice(`Đã gửi yêu cầu bảo hành #${created.shortCode}. Cửa hàng sẽ xem xét và thông báo kết quả.`)
        try {
            setData(await fetchAll())   // tải lại: sản phẩm vừa gửi chuyển sang "Đang chờ xử lý", yêu cầu mới hiện trong danh sách
        } catch (err) {
            handleLoadError(err)
        }
    }

    if (error) return <p className="warranty__message warranty__message--error">{error}</p>
    if (!data) return <p className="warranty__message">Đang tải...</p>

    return (
        <section className="warranty">
            <h1>Bảo hành</h1>
            <p className="warranty__rule">
                Xe máy bảo hành <strong>2 năm</strong>, phụ tùng <strong>6 tháng</strong> kể từ ngày mua. Chỉ gửi được yêu cầu
                cho sản phẩm trong đơn hàng đã hoàn thành.
            </p>
            {notice && <p className="order-detail__success" role="status">{notice}</p>}

            <h2>Sản phẩm đã mua</h2>
            {data.options.length === 0 ? (
                <p className="warranty__message">
                    Bạn chưa có sản phẩm nào trong đơn hàng đã hoàn thành. <Link to="/orders">Xem đơn hàng</Link>
                </p>
            ) : (
                <ul className="warranty__list">
                    {data.options.map((o) => (
                        <li key={o.orderItemId} className="warranty-card">
                            <div className="warranty-card__head">
                                <div>
                                    <strong>{o.productName}</strong>
                                    <span className="warranty-card__sub">
                                        Đơn #{o.orderCode} · mua {formatDateTime(o.purchasedAt)} · bảo hành đến {formatDateTime(o.deadline)}
                                    </span>
                                </div>
                                {o.expired && <span className="status-badge status-badge--cancelled">Hết hạn bảo hành</span>}
                                {!o.expired && o.pending && <span className="status-badge status-badge--pending">Đang chờ xử lý</span>}
                                {o.canRequest && selected !== o.orderItemId && (
                                    <button type="button" className="warranty__btn warranty__btn--primary"
                                            onClick={() => { setSelected(o.orderItemId); setNotice('') }}>
                                        Gửi yêu cầu bảo hành
                                    </button>
                                )}
                            </div>
                            {selected === o.orderItemId && (
                                <WarrantyForm orderItemId={o.orderItemId} onDone={submitted} onCancel={() => setSelected(null)} logout={logout} />
                            )}
                        </li>
                    ))}
                </ul>
            )}

            <h2>Yêu cầu của tôi</h2>
            {data.requests.length === 0 ? (
                <p className="warranty__message">Bạn chưa gửi yêu cầu bảo hành nào.</p>
            ) : (
                <ul className="warranty__list">
                    {data.requests.map((r) => (
                        <li key={r.requestId} className="warranty-card">
                            <div className="warranty-card__head">
                                <div>
                                    <strong>{r.productName}</strong>
                                    <span className="warranty-card__sub">
                                        Yêu cầu #{r.shortCode} · đơn #{r.orderCode} · gửi {formatDateTime(r.submittedAt)}
                                    </span>
                                </div>
                                <span className={`status-badge status-badge--${r.status.toLowerCase()}`}>{r.statusLabel}</span>
                            </div>
                            <p className="warranty-card__text">{r.description}</p>
                            {r.attachmentUrl && (
                                <p className="warranty-card__sub">
                                    Minh chứng: <a href={r.attachmentUrl} target="_blank" rel="noopener noreferrer">{r.attachmentUrl}</a>
                                </p>
                            )}
                            {r.resolvedAt && (
                                <p className="warranty-card__result">
                                    {r.status === 'REJECTED' ? 'Lý do từ chối' : 'Ghi chú của cửa hàng'}
                                    {r.decisionReason ? `: ${r.decisionReason}` : ': (không có)'} — xử lý {formatDateTime(r.resolvedAt)}
                                </p>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </section>
    )
}

function WarrantyForm({ orderItemId, onDone, onCancel, logout }) {
    const [description, setDescription] = useState('')
    const [attachmentUrl, setAttachmentUrl] = useState('')
    const [error, setError] = useState('')
    const [fieldErrors, setFieldErrors] = useState({})
    const [submitting, setSubmitting] = useState(false)

    async function handleSubmit(e) {
        e.preventDefault()
        setError('')
        setFieldErrors({})
        if (!description.trim()) {
            setFieldErrors({ description: 'Vui lòng mô tả lỗi.' })
            return
        }
        setSubmitting(true)
        try {
            const res = await client.post('/warranty-requests', {
                orderItemId,
                description: description.trim(),
                attachmentUrl: attachmentUrl.trim() || null,
            })
            await onDone(res.data)
        } catch (err) {
            if (err.response?.status === 401) { logout(); return }
            const errors = err.response?.data?.errors ?? {}
            setFieldErrors(errors)
            setError(Object.keys(errors).length > 0 ? '' : apiError(err, 'Không gửi được yêu cầu.'))
            setSubmitting(false)
        }
    }

    return (
        <form className="warranty-form" onSubmit={handleSubmit} noValidate>
            <label>
                Mô tả lỗi
                <textarea rows="3" maxLength={1000} value={description} onChange={(e) => setDescription(e.target.value)}
                          placeholder="Sản phẩm bị lỗi gì, xuất hiện từ khi nào..." />
                {fieldErrors.description && <span className="warranty-form__error">{fieldErrors.description}</span>}
            </label>
            <label>
                Đường dẫn ảnh hoặc video minh chứng (không bắt buộc)
                <input type="url" maxLength={500} value={attachmentUrl} onChange={(e) => setAttachmentUrl(e.target.value)}
                       placeholder="https://..." />
                {fieldErrors.attachmentUrl && <span className="warranty-form__error">{fieldErrors.attachmentUrl}</span>}
            </label>
            {error && <p className="warranty-form__alert" role="alert">{error}</p>}
            <div className="warranty-form__actions">
                <button type="submit" className="warranty__btn warranty__btn--primary" disabled={submitting}>
                    {submitting ? 'Đang gửi...' : 'Gửi yêu cầu'}
                </button>
                <button type="button" className="warranty__btn" onClick={onCancel} disabled={submitting}>Hủy</button>
            </div>
        </form>
    )
}
