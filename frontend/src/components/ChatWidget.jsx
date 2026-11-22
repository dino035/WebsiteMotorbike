import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client.js'
import ChatMessageList from './ChatMessageList.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import './ChatWidget.css'

const MAX_LENGTH = 500
// Trợ lý gọi Gemini hai lần nên có thể lâu hơn mức 15 giây mặc định của client.
const SEND_TIMEOUT_MS = 60000

const FAB = 56            // đường kính nút tròn
const GAP = 12            // khoảng cách giữa nút và khung
const EDGE = 16           // chừa mép màn hình
const RIGHT = 24          // khoảng cách cố định tới lề phải
const DRAG_SLOP = 4       // di chuyển dưới ngần này pixel vẫn tính là một cú bấm
const Y_KEY = 'motoparts.chatY'

/** Đọc độ cao đã lưu. Trình duyệt chặn localStorage thì coi như chưa có. */
function readY() {
    try {
        const raw = localStorage.getItem(Y_KEY)
        const y = raw === null ? NaN : Number(raw)
        if (Number.isFinite(y)) return y
    } catch {
        // bỏ qua: chỉ là tiện ích, không có cũng chạy bình thường
    }
    return null
}

function saveY(y) {
    try {
        localStorage.setItem(Y_KEY, String(y))
    } catch {
        // bỏ qua
    }
}

/**
 * Kích thước vùng nội dung. Dùng clientWidth/clientHeight của <html> chứ không window.innerWidth:
 * innerWidth tính cả thanh cuộn, lấy nó thì nút và khung bị đẩy nằm đè lên thanh cuộn.
 */
function viewportSize() {
    const el = document.documentElement
    return { w: el.clientWidth, h: el.clientHeight }
}

/** Giữ nút nằm trong màn hình, kể cả sau khi đổi kích thước cửa sổ. */
function clampY(y) {
    const max = Math.max(EDGE, viewportSize().h - FAB - EDGE)
    return Math.min(Math.max(y, EDGE), max)
}

function defaultY() {
    return viewportSize().h - FAB - 24
}

/**
 * Khung chat nổi bám lề phải, kéo lên xuống được, hiện trên mọi trang cho khách đã đăng nhập.
 * Nó nối tiếp cuộc trò chuyện gần nhất; muốn xem lại toàn bộ lịch sử thì vào trang /chat.
 * Admin không thấy khung này: tư vấn mua hàng là việc của khách.
 */
export default function ChatWidget() {
    const { token, user, logout } = useAuth()
    const isCustomer = Boolean(token) && user?.role !== 'ADMIN'

    const [open, setOpen] = useState(false)
    const [y, setY] = useState(() => clampY(readY() ?? defaultY()))
    const [viewport, setViewport] = useState(viewportSize)

    const [loaded, setLoaded] = useState(false)       // đã nạp cuộc gần nhất chưa
    const [sessionId, setSessionId] = useState('')
    const [vehicle, setVehicle] = useState(null)
    const [profileId, setProfileId] = useState('')    // xe mặc định cho cuộc mới
    const [messages, setMessages] = useState([])
    const [text, setText] = useState('')
    const [sending, setSending] = useState(false)
    const [error, setError] = useState('')

    const logRef = useRef(null)
    const yRef = useRef(y)            // độ cao mới nhất, dùng lúc thả chuột để lưu
    const dragRef = useRef(null)

    const moveY = useCallback((next) => {
        const safe = clampY(next)
        yRef.current = safe
        setY(safe)
    }, [])

    // Thu nhỏ cửa sổ thì kéo nút vào lại trong màn hình
    useEffect(() => {
        function onResize() {
            setViewport(viewportSize())
            moveY(yRef.current)
        }
        window.addEventListener('resize', onResize)
        return () => window.removeEventListener('resize', onResize)
    }, [moveY])

    // Chỉ gọi API khi khách mở khung lần đầu, không tải sẵn ở mọi trang.
    useEffect(() => {
        if (!open || loaded || !isCustomer) return undefined
        let ignore = false

        async function load() {
            try {
                const [sessions, profiles] = await Promise.all([
                    client.get('/chat-sessions'),
                    client.get('/vehicle-profiles'),
                ])
                if (ignore) return
                setProfileId(profiles.data[0]?.profileId ?? '')

                const newest = sessions.data[0]
                if (newest) {
                    const detail = await client.get(`/chat-sessions/${encodeURIComponent(newest.sessionId)}`)
                    if (ignore) return
                    setSessionId(newest.sessionId)
                    setMessages(detail.data.messages)
                    setVehicle(detail.data.vehicle ?? null)
                }
            } catch (err) {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setError(apiError(err, 'Không tải được cuộc trò chuyện.'))
            } finally {
                if (!ignore) setLoaded(true)
            }
        }

        load()
        return () => { ignore = true }
    }, [open, loaded, isCustomer, logout])

    // Cuộn xuống tin mới nhất
    useEffect(() => {
        const el = logRef.current
        if (el) el.scrollTop = el.scrollHeight
    }, [messages.length, sending, open])

    // Esc đóng khung
    useEffect(() => {
        if (!open) return undefined
        function onKey(e) { if (e.key === 'Escape') setOpen(false) }
        window.addEventListener('keydown', onKey)
        return () => window.removeEventListener('keydown', onKey)
    }, [open])

    if (!isCustomer) return null

    // ---------------------------------------------------------------- kéo thả

    function startDrag(event, { toggleOnClick }) {
        if (event.button !== 0) return          // chỉ nút chuột trái
        event.currentTarget.setPointerCapture(event.pointerId)
        dragRef.current = { dy: event.clientY - yRef.current, moved: false, toggleOnClick }
    }

    function onDragMove(event) {
        const drag = dragRef.current
        if (!drag) return
        const next = event.clientY - drag.dy
        if (!drag.moved) {
            if (Math.abs(next - yRef.current) <= DRAG_SLOP) return   // chưa đủ xa: vẫn coi là đang bấm
            drag.moved = true
        }
        moveY(next)
    }

    function endDrag() {
        const drag = dragRef.current
        dragRef.current = null
        if (!drag) return
        if (drag.moved) saveY(yRef.current)
        else if (drag.toggleOnClick) setOpen((v) => !v)
    }

    // Nút khóa ở lề phải; khung canh cùng mép phải với nút, nằm trên nếu còn chỗ, không thì nằm dưới.
    const fabX = Math.max(EDGE, viewport.w - FAB - RIGHT)
    const panelW = Math.min(380, viewport.w - EDGE * 2)
    const panelH = Math.min(540, viewport.h - 140)
    const left = Math.max(EDGE, fabX + FAB - panelW)
    const above = y - GAP - panelH >= EDGE
    const top = above ? y - GAP - panelH : Math.min(y + FAB + GAP, viewport.h - panelH - EDGE)

    // ---------------------------------------------------------------- gửi tin

    async function send(event) {
        event.preventDefault()
        const content = text.trim()
        if (!content || sending) return

        const base = messages
        const optimistic = {
            messageId: `tmp-${Date.now()}`, sender: 'CUSTOMER',
            content, sentAt: null, status: null, products: [],
        }
        setSending(true)
        setError('')
        setMessages([...base, optimistic])
        setText('')

        let id = sessionId
        try {
            if (!id) {
                const created = await client.post('/chat-sessions', profileId ? { profileId } : undefined)
                id = created.data.sessionId
                setSessionId(id)
                setVehicle(created.data.vehicle ?? null)
            }
            const res = await client.post(
                `/chat-sessions/${encodeURIComponent(id)}/messages`,
                { content },
                { timeout: SEND_TIMEOUT_MS },
            )
            setMessages([...base, res.data.customerMessage, res.data.assistantMessage])
        } catch (err) {
            if (err.response?.status === 401) { logout(); return }
            setMessages(base)          // bỏ dòng tạm
            setText(content)           // trả lại chữ để khách gửi lại
            setError(err.code === 'ECONNABORTED'
                ? 'Trợ lý trả lời quá lâu. Mở trang Hỏi AI để xem câu trả lời nếu đã có.'
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

    function startNew() {
        setSessionId('')
        setMessages([])
        setVehicle(null)
        setError('')
    }

    return (
        <>
            <button
                type="button"
                className={open ? 'chat-fab chat-fab--open' : 'chat-fab'}
                style={{ left: fabX, top: y }}
                aria-label={open ? 'Đóng khung tư vấn' : 'Mở khung tư vấn'}
                aria-expanded={open}
                onPointerDown={(e) => startDrag(e, { toggleOnClick: true })}
                onPointerMove={onDragMove}
                onPointerUp={endDrag}
                onPointerCancel={endDrag}
            >
                {open ? <CloseIcon /> : <ChatIcon />}
            </button>

            {open && (
                <section className="chat-widget" style={{ left, top, width: panelW, height: panelH }}
                         aria-label="Tư vấn mua hàng">
                    <header
                        className="chat-widget__head"
                        onPointerDown={(e) => startDrag(e, { toggleOnClick: false })}
                        onPointerMove={onDragMove}
                        onPointerUp={endDrag}
                        onPointerCancel={endDrag}
                    >
                        <div className="chat-widget__title">
                            <strong>Trợ lý MotoParts</strong>
                            <span>{vehicle ? `Đang tư vấn cho ${vehicle.label}` : 'Kéo lên xuống để di chuyển'}</span>
                        </div>
                        <button type="button" className="chat-widget__new" disabled={sending}
                                title="Bắt đầu một cuộc trò chuyện mới"
                                onPointerDown={(e) => e.stopPropagation()} onClick={startNew}>
                            + Trò chuyện mới
                        </button>
                    </header>

                    <div className="chat-widget__log" ref={logRef}>
                        {!loaded && <p className="chat-widget__note">Đang tải...</p>}
                        {loaded && messages.length === 0 && !error && (
                            <p className="chat-widget__note">
                                Cho biết xe và nhu cầu của bạn, trợ lý sẽ gợi ý sản phẩm đang bán trong cửa hàng.
                            </p>
                        )}
                        <ChatMessageList messages={messages} pending={sending} />
                    </div>

                    {error && <p className="chat-widget__error" role="alert">{error}</p>}

                    <form className="chat-widget__form" onSubmit={send}>
                        <textarea
                            value={text}
                            rows={2}
                            maxLength={MAX_LENGTH}
                            disabled={sending || !loaded}
                            placeholder="Ví dụ: nhớt cho Honda Wave 2015 dưới 200k"
                            aria-label="Câu hỏi của bạn"
                            onChange={(e) => setText(e.target.value)}
                            onKeyDown={onKeyDown}
                        />
                        <button type="submit" disabled={sending || !loaded || text.trim() === ''}>Gửi</button>
                    </form>

                    <footer className="chat-widget__foot">
                        <Link to="/chat" onClick={() => setOpen(false)}>Xem tất cả cuộc trò chuyện →</Link>
                    </footer>
                </section>
            )}
        </>
    )
}

function ChatIcon() {
    return (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.5 8.5 0 0 1-3.8-.9L3 21l1.9-5.1A8.4 8.4 0 0 1 12 3a8.4 8.4 0 0 1 9 8.5z" />
        </svg>
    )
}

function CloseIcon() {
    return (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <path d="M18 6 6 18M6 6l12 12" />
        </svg>
    )
}
