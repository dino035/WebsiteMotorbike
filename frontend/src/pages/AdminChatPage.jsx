import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime } from '../utils/format.js'
import './AdminPages.css'
import '../components/ChatMessageList.css'

const FILTERS = [
    { value: 'UNANSWERED', label: 'Chưa trả lời được' },
    { value: 'ANSWERED', label: 'Đã trả lời' },
    { value: 'ALL', label: 'Tất cả' },
]

/**
 * Admin: các câu hỏi của khách với chat box. Mặc định hiện câu chưa trả lời được, để biết khách đang cần gì mà cửa hàng chưa có.
 * Bộ lọc và trang nằm trên địa chỉ nên tải lại vẫn giữ.
 */
export default function AdminChatPage() {
    const { logout } = useAuth()
    const [params, setParams] = useSearchParams()
    const rawStatus = params.get('status')
    const status = FILTERS.some((f) => f.value === rawStatus) ? rawStatus : 'UNANSWERED'
    const page = Math.max(Number.parseInt(params.get('page') || '0', 10) || 0, 0)

    const [result, setResult] = useState({ key: null, data: null, error: '' })
    const key = `${status}|${page}`

    useEffect(() => {
        let ignore = false
        client.get('/admin/chat/questions', { params: { status: status === 'ALL' ? undefined : status, page, size: 15 } })
            .then((res) => { if (!ignore) setResult({ key, data: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setResult({ key, data: null, error: apiError(err, 'Không tải được danh sách câu hỏi.') })
            })
        return () => { ignore = true }
    }, [status, page, key, logout])

    function chooseStatus(value) {
        setParams(value === 'UNANSWERED' ? {} : { status: value })
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
                {FILTERS.map((f) => (
                    <button key={f.value} type="button"
                            className={status === f.value ? 'admin__filter admin__filter--on' : 'admin__filter'}
                            onClick={() => chooseStatus(f.value)}>{f.label}</button>
                ))}
            </div>

            {loading && <p className="admin__message">Đang tải...</p>}
            {!loading && result.error && <p className="admin__message admin__message--error">{result.error}</p>}

            {!loading && data && (
                data.items.length === 0 ? (
                    <p className="admin__message">Không có câu hỏi nào.</p>
                ) : (
                    <>
                        <div className="admin__table-wrap">
                            <table className="admin__table">
                                <thead>
                                    <tr><th>Lúc hỏi</th><th>Khách</th><th>Câu hỏi</th><th>Trạng thái</th><th></th></tr>
                                </thead>
                                <tbody>
                                    {data.items.map((q) => (
                                        <tr key={q.messageId}>
                                            <td>{formatDateTime(q.sentAt)}</td>
                                            <td>{q.customerEmail}</td>
                                            <td className="admin__question">{q.content}</td>
                                            <td>
                                                <span className={q.status === 'ANSWERED' ? 'chat-status chat-status--answered' : 'chat-status chat-status--unanswered'}>
                                                    {q.status === 'ANSWERED' ? 'Đã trả lời' : 'Chưa trả lời được'}
                                                </span>
                                            </td>
                                            <td><Link to={`/admin/chat/${q.sessionId}`}>Xem cuộc trò chuyện</Link></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <div className="admin__pager">
                            <button type="button" disabled={data.page <= 0} onClick={() => goToPage(data.page - 1)}>← Trước</button>
                            <span>Trang {data.page + 1} / {Math.max(data.totalPages, 1)} · {data.totalItems} câu hỏi</span>
                            <button type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => goToPage(data.page + 1)}>Sau →</button>
                        </div>
                    </>
                )
            )}
        </section>
    )
}
