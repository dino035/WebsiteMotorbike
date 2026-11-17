import ProductCard from './ProductCard.jsx'
import { formatDateTime } from '../utils/format.js'
import './ChatMessageList.css'

const STATUS_LABEL = { ANSWERED: 'Đã trả lời', UNANSWERED: 'Chưa trả lời được' }

/**
 * Danh sách tin nhắn của một cuộc trò chuyện. Câu trả lời của trợ lý kèm thẻ sản phẩm thật (bấm vào để xem chi tiết).
 * pending: hiện dòng "đang trả lời". showStatus: Admin xem trạng thái trả lời của từng câu hỏi (khách không thấy).
 */
export default function ChatMessageList({ messages, pending = false, showStatus = false }) {
    return (
        <ol className="chat-log" aria-live="polite">
            {messages.map((m) => {
                const fromCustomer = m.sender === 'CUSTOMER'
                return (
                    <li key={m.messageId} className={fromCustomer ? 'chat-msg chat-msg--customer' : 'chat-msg chat-msg--assistant'}>
                        <div className="chat-msg__bubble">{m.content}</div>
                        {m.products?.length > 0 && (
                            <div className="chat-products">
                                {m.products.map((p) => <ProductCard key={p.productId} product={p} />)}
                            </div>
                        )}
                        <div className="chat-msg__meta">
                            {m.sentAt && <span>{formatDateTime(m.sentAt)}</span>}
                            {showStatus && fromCustomer && m.status && (
                                <span className={m.status === 'ANSWERED' ? 'chat-status chat-status--answered' : 'chat-status chat-status--unanswered'}>
                                    {STATUS_LABEL[m.status] ?? m.status}
                                </span>
                            )}
                        </div>
                    </li>
                )
            })}
            {pending && (
                <li className="chat-msg chat-msg--assistant">
                    <div className="chat-msg__bubble chat-msg__bubble--typing">Trợ lý đang trả lời...</div>
                </li>
            )}
        </ol>
    )
}
