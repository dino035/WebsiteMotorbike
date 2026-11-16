import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import client from '../api/client.js'
import useAuth from '../hooks/useAuth.js'
import useNotifications from '../hooks/useNotifications.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime } from '../utils/format.js'
import './NotificationsPage.css'

/** Thông báo của tôi: mới nhất trước, phân trang; bấm vào một thông báo để đánh dấu đã đọc, hoặc đánh dấu tất cả. */
export default function NotificationsPage() {
    const { logout } = useAuth()
    const { unreadCount, refresh: refreshCount } = useNotifications()
    const [params, setParams] = useSearchParams()
    const page = Math.max(Number.parseInt(params.get('page') || '0', 10) || 0, 0)

    const [result, setResult] = useState({ page: null, data: null, error: '' })
    const [actionError, setActionError] = useState('')
    const [busy, setBusy] = useState(false)

    useEffect(() => {
        let ignore = false
        client.get('/notifications', { params: { page, size: 15 } })
            .then((res) => { if (!ignore) setResult({ page, data: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setResult({ page, data: null, error: apiError(err, 'Không tải được thông báo.') })
            })
        return () => { ignore = true }
    }, [page, logout])

    async function run(action) {
        setBusy(true)
        setActionError('')
        try {
            await action()
            await refreshCount()
        } catch (err) {
            if (err.response?.status === 401) logout()
            else setActionError(apiError(err, 'Không cập nhật được thông báo.'))
        } finally {
            setBusy(false)
        }
    }

    function markRead(id) {
        return run(async () => {
            await client.post(`/notifications/${encodeURIComponent(id)}/read`)
            setResult((r) => r.data
                ? { ...r, data: { ...r.data, items: r.data.items.map((n) => (n.notificationId === id ? { ...n, isRead: true } : n)) } }
                : r)
        })
    }

    function markAllRead() {
        return run(async () => {
            await client.post('/notifications/read-all')
            setResult((r) => r.data
                ? { ...r, data: { ...r.data, items: r.data.items.map((n) => ({ ...n, isRead: true })) } }
                : r)
        })
    }

    function goToPage(p) {
        setParams(p > 0 ? { page: String(p) } : {})
    }

    const loading = result.page !== page
    const data = result.data

    return (
        <section className="notifications">
            <div className="notifications__head">
                <h1>Thông báo</h1>
                <button type="button" className="notifications__all" disabled={busy || unreadCount === 0} onClick={markAllRead}>
                    Đánh dấu tất cả đã đọc
                </button>
            </div>

            {loading && <p className="notifications__message">Đang tải...</p>}
            {!loading && result.error && <p className="notifications__message notifications__message--error">{result.error}</p>}
            {actionError && <p className="notifications__message notifications__message--error" role="alert">{actionError}</p>}

            {!loading && data && (
                data.items.length === 0 ? (
                    <p className="notifications__message">Bạn chưa có thông báo nào.</p>
                ) : (
                    <>
                        <ul className="notifications__list">
                            {data.items.map((n) => (
                                <li key={n.notificationId}>
                                    <button type="button" disabled={busy}
                                            className={n.isRead ? 'notification' : 'notification notification--unread'}
                                            onClick={() => { if (!n.isRead) markRead(n.notificationId) }}
                                            aria-label={`${n.isRead ? '' : 'Chưa đọc: '}${n.typeLabel}. ${n.content}`}>
                                        <span className="notification__top">
                                            <strong>{n.typeLabel}</strong>
                                            <span className="notification__time">{formatDateTime(n.createdAt)}</span>
                                        </span>
                                        <span className="notification__content">{n.content}</span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                        <div className="notifications__pager">
                            <button type="button" disabled={data.page <= 0} onClick={() => goToPage(data.page - 1)}>← Trước</button>
                            <span>Trang {data.page + 1} / {Math.max(data.totalPages, 1)} · {data.totalItems} thông báo</span>
                            <button type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => goToPage(data.page + 1)}>Sau →</button>
                        </div>
                    </>
                )
            )}
        </section>
    )
}
