import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import client from '../api/client.js'
import ChatMessageList from '../components/ChatMessageList.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime } from '../utils/format.js'
import './ChatPage.css'

const MAX_LENGTH = 500
// Trợ lý có thể mất vài giây (hai lần gọi Gemini), lâu hơn mức 15 giây mặc định của client.
const SEND_TIMEOUT_MS = 60000
const EXAMPLES = [
    'Nhớt cho Honda Wave 2015 dưới 200k',
    'Xe số đời 2018 trở lên dưới 20 triệu',
    'Lọc gió cho Yamaha Exciter 2012',
]
const EMPTY = { id: '', messages: [], error: '' }

/**
 * Chat box AI: hỏi bằng tiếng Việt, trợ lý gợi ý sản phẩm thật của cửa hàng.
 * Cuộc trò chuyện đang mở nằm trên địa chỉ (?s=mã) nên tải lại vẫn giữ. Cuộc mới chỉ được tạo trên máy chủ khi khách gửi câu đầu tiên.
 */
export default function ChatPage() {
    const { logout } = useAuth()
    const [params, setParams] = useSearchParams()
    const activeId = params.get('s') || ''

    const [sessions, setSessions] = useState([])
    const [listVersion, setListVersion] = useState(0)
    // conv.id là cuộc trò chuyện mà conv.messages thuộc về ('' = cuộc mới chưa có trên máy chủ)
    const [conv, setConv] = useState(EMPTY)
    const [text, setText] = useState('')
    const [sending, setSending] = useState(false)
    const [sendError, setSendError] = useState('')
    const loadedRef = useRef('')   // cuộc trò chuyện đã có tin nhắn trong conv, không cần tải lại
    const logRef = useRef(null)

    useEffect(() => {
        let ignore = false
        client.get('/chat-sessions')
            .then((res) => { if (!ignore) setSessions(res.data) })
            .catch((err) => { if (err.response?.status === 401) logout() })
        return () => { ignore = true }
    }, [listVersion, logout])

    useEffect(() => {
        if (!activeId || loadedRef.current === activeId) return undefined
        let ignore = false
        client.get(`/chat-sessions/${encodeURIComponent(activeId)}`)
            .then((res) => {
                if (ignore) return
                loadedRef.current = activeId
                setConv({ id: activeId, messages: res.data.messages, error: '' })
            })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setConv({ id: activeId, messages: [], error: apiError(err, 'Không tải được cuộc trò chuyện này.') })
            })
        return () => { ignore = true }
    }, [activeId, logout])

    // Cuộn xuống tin mới nhất
    const shownCount = conv.messages.length
    useEffect(() => {
        const el = logRef.current
        if (el) el.scrollTop = el.scrollHeight
    }, [shownCount, sending, activeId])

    const loading = Boolean(activeId) && conv.id !== activeId
    // Quay lại (nút Back) về cuộc mới trong lúc conv còn giữ cuộc cũ: coi như cuộc trống
    const current = activeId ? conv : (conv.id === '' ? conv : EMPTY)

    function startNew() {
        loadedRef.current = ''
        setConv(EMPTY)
        setSendError('')
        setParams({})
    }

    function openSession(id) {
        if (id === activeId) return
        setSendError('')
        setParams({ s: id })
    }

    async function send(event) {
        event.preventDefault()
        const content = text.trim()
        if (!content || sending) return
        if (content.length > MAX_LENGTH) {
            setSendError(`Câu hỏi tối đa ${MAX_LENGTH} ký tự.`)
            return
        }

        const base = current.messages
        const optimistic = { messageId: `tmp-${Date.now()}`, sender: 'CUSTOMER', content, sentAt: null, status: null, products: [] }
        let sessionId = activeId
        setSending(true)
        setSendError('')
        setConv({ id: activeId, messages: [...base, optimistic], error: '' })
        setText('')
        try {
            if (!sessionId) {
                const created = await client.post('/chat-sessions')
                sessionId = created.data.sessionId
            }
            const res = await client.post(`/chat-sessions/${encodeURIComponent(sessionId)}/messages`, { content }, { timeout: SEND_TIMEOUT_MS })
            loadedRef.current = sessionId
            setConv({ id: sessionId, messages: [...base, res.data.customerMessage, res.data.assistantMessage], error: '' })
            if (sessionId !== activeId) setParams({ s: sessionId })
            setListVersion((v) => v + 1)
        } catch (err) {
            if (err.response?.status === 401) {
                logout()
                return
            }
            // câu hỏi chưa được trả lời: bỏ dòng tạm, trả lại chữ để khách gửi lại
            if (sessionId && sessionId !== activeId) {
                // cuộc mới đã được tạo trên máy chủ: chuyển sang dùng nó
                loadedRef.current = sessionId
                setConv({ id: sessionId, messages: base, error: '' })
                setParams({ s: sessionId })
                setListVersion((v) => v + 1)
            } else {
                setConv({ id: activeId, messages: base, error: '' })
            }
            setText(content)
            setSendError(err.code === 'ECONNABORTED'
                ? 'Trợ lý trả lời quá lâu. Hãy tải lại trang, nếu đã có câu trả lời thì nó nằm trong cuộc trò chuyện.'
                : apiError(err, 'Không gửi được câu hỏi.'))
        } finally {
            setSending(false)
        }
    }

    function onKeyDown(event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault()
            event.currentTarget.form.requestSubmit()
        }
    }

    const showExamples = !loading && !current.error && current.messages.length === 0

    return (
        <section className="chat">
            <aside className="chat__side" aria-label="Các cuộc trò chuyện">
                <button type="button" className="chat__new" disabled={sending} onClick={startNew}>+ Cuộc trò chuyện mới</button>
                <ul className="chat__sessions">
                    {sessions.map((s) => (
                        <li key={s.sessionId}>
                            <button type="button" disabled={sending}
                                    className={s.sessionId === activeId ? 'chat__session chat__session--on' : 'chat__session'}
                                    onClick={() => openSession(s.sessionId)}>
                                <span className="chat__session-title">{s.title || 'Cuộc trò chuyện trống'}</span>
                                <span className="chat__session-time">{formatDateTime(s.startedAt)}</span>
                            </button>
                        </li>
                    ))}
                </ul>
            </aside>

            <div className="chat__panel">
                <div className="chat__log" ref={logRef}>
                    {loading && <p className="chat__note">Đang tải...</p>}
                    {!loading && current.error && <p className="chat__note chat__note--error">{current.error}</p>}
                    {showExamples && (
                        <div className="chat__welcome">
                            <h1>Hỏi trợ lý MotoParts</h1>
                            <p>Cho biết xe và nhu cầu của bạn, trợ lý sẽ gợi ý sản phẩm đang bán trong cửa hàng.</p>
                            <div className="chat__examples">
                                {EXAMPLES.map((q) => (
                                    <button key={q} type="button" onClick={() => setText(q)}>{q}</button>
                                ))}
                            </div>
                        </div>
                    )}
                    {!loading && <ChatMessageList messages={current.messages} pending={sending} />}
                </div>

                {sendError && <p className="chat__note chat__note--error chat__send-error" role="alert">{sendError}</p>}
                <form className="chat__form" onSubmit={send}>
                    <textarea value={text} rows={2} maxLength={MAX_LENGTH} disabled={sending || loading}
                              placeholder="Ví dụ: nhớt cho Honda Wave 2015 dưới 200k"
                              aria-label="Câu hỏi của bạn"
                              onChange={(e) => setText(e.target.value)} onKeyDown={onKeyDown} />
                    <div className="chat__form-side">
                        <span className="chat__count">{text.length}/{MAX_LENGTH}</span>
                        <button type="submit" disabled={sending || loading || text.trim() === ''}>Gửi</button>
                    </div>
                </form>
            </div>
        </section>
    )
}
