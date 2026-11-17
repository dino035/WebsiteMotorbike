import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import ChatMessageList from '../components/ChatMessageList.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime } from '../utils/format.js'
import './AdminPages.css'

/** Admin: đọc lại một cuộc trò chuyện của khách (chỉ xem), kèm trạng thái trả lời của từng câu hỏi. */
export default function AdminChatSessionPage() {
    const { id } = useParams()
    const { logout } = useAuth()
    const [result, setResult] = useState({ id: null, data: null, error: '' })

    useEffect(() => {
        let ignore = false
        client.get(`/admin/chat/sessions/${encodeURIComponent(id)}`)
            .then((res) => { if (!ignore) setResult({ id, data: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setResult({ id, data: null, error: apiError(err, 'Không tải được cuộc trò chuyện.') })
            })
        return () => { ignore = true }
    }, [id, logout])

    const loading = result.id !== id
    const data = result.data

    return (
        <section className="admin">
            <h1>Quản trị</h1>
            <AdminNav />
            <p><Link to="/admin/chat">← Danh sách câu hỏi</Link></p>

            {loading && <p className="admin__message">Đang tải...</p>}
            {!loading && result.error && <p className="admin__message admin__message--error">{result.error}</p>}

            {!loading && data && (
                <>
                    <h2 className="admin__title">Cuộc trò chuyện của {data.customerEmail}</h2>
                    <p className="admin__sub">Bắt đầu {formatDateTime(data.session.startedAt)}</p>
                    {data.session.messages.length === 0
                        ? <p className="admin__message">Cuộc trò chuyện chưa có tin nhắn.</p>
                        : <ChatMessageList messages={data.session.messages} showStatus />}
                </>
            )}
        </section>
    )
}
