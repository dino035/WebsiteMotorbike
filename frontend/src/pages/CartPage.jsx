import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import useCart from '../hooks/useCart.js'
import { apiError } from '../utils/apiError.js'
import { formatPrice, formatTime } from '../utils/format.js'
import './CartPage.css'

export default function CartPage() {
    const { cart, refresh, updateQuantity, remove } = useCart()
    const [loading, setLoading] = useState(true)
    const [loadError, setLoadError] = useState('')
    const [notices, setNotices] = useState([])   // dòng vừa bị gỡ vì hết hàng / xe đã có người khác giữ
    const [actionError, setActionError] = useState('')
    const [busyId, setBusyId] = useState(null)

    // Mở trang: tải giỏ mới nhất. Backend gỡ dòng không còn khả dụng và báo trong notices.
    useEffect(() => {
        let ignore = false
        refresh()
            .then((data) => { if (!ignore) setNotices(data.notices) })
            .catch((err) => { if (!ignore) setLoadError(apiError(err, 'Không tải được giỏ hàng.')) })
            .finally(() => { if (!ignore) setLoading(false) })
        return () => { ignore = true }
    }, [refresh])

    async function run(cartItemId, action) {
        setBusyId(cartItemId)
        setActionError('')
        try {
            const data = await action()
            setNotices(data.notices)
        } catch (err) {
            setActionError(apiError(err))
        } finally {
            setBusyId(null)
        }
    }

    const changeQuantity = (line, value) => {
        const n = Math.min(99, Math.max(1, Number.parseInt(value, 10) || 1))
        if (n !== line.quantity) run(line.cartItemId, () => updateQuantity(line.cartItemId, n))
    }

    if (loading && !cart) return <p className="cart__message">Đang tải giỏ hàng...</p>
    if (loadError && !cart) return <p className="cart__message cart__message--error">{loadError}</p>
    if (!cart) return null

    return (
        <section className="cart">
            <h1>Giỏ hàng</h1>

            {notices.length > 0 && (
                <div className="cart__notices" role="status">
                    {notices.map((text) => <p key={text}>{text}</p>)}
                </div>
            )}
            {actionError && <p className="cart__message cart__message--error" role="alert">{actionError}</p>}

            {cart.lines.length === 0 ? (
                <div className="cart__empty">
                    <p>Giỏ hàng của bạn đang trống.</p>
                    <Link to="/products" className="cart__link-button">Xem sản phẩm</Link>
                </div>
            ) : (
                <div className="cart__layout">
                    <ul className="cart__lines">
                        {cart.lines.map((line) => {
                            const busy = busyId === line.cartItemId
                            return (
                                <li key={line.cartItemId} className="cart-line">
                                    <Link to={`/products/${line.productId}`} className="cart-line__image">
                                        {line.thumbnailUrl
                                            ? <img src={line.thumbnailUrl} alt={line.name} />
                                            : <span>Chưa có ảnh</span>}
                                    </Link>

                                    <div className="cart-line__info">
                                        <span className="cart-line__type">{line.productType}</span>
                                        <Link to={`/products/${line.productId}`} className="cart-line__name">{line.name}</Link>
                                        <span className="cart-line__unit">{formatPrice(line.unitPrice)}</span>
                                        {line.motorbike && line.holdExpiresAt && (
                                            <span className="cart-line__hold">
                                                Đang giữ xe cho bạn đến {formatTime(line.holdExpiresAt)}
                                            </span>
                                        )}
                                    </div>

                                    <div className="cart-line__qty">
                                        {line.motorbike ? (
                                            <span>1 chiếc</span>
                                        ) : (
                                            <div className="stepper">
                                                <button type="button" aria-label="Giảm" disabled={busy || line.quantity <= 1}
                                                        onClick={() => changeQuantity(line, line.quantity - 1)}>−</button>
                                                {/* key theo số lượng: sau khi cập nhật ô nhập được dựng lại với số mới */}
                                                <input key={line.quantity} type="number" min="1" max="99"
                                                       defaultValue={line.quantity} disabled={busy}
                                                       aria-label={`Số lượng ${line.name}`}
                                                       onBlur={(e) => changeQuantity(line, e.target.value)}
                                                       onKeyDown={(e) => { if (e.key === 'Enter') e.target.blur() }} />
                                                <button type="button" aria-label="Tăng" disabled={busy || line.quantity >= 99}
                                                        onClick={() => changeQuantity(line, line.quantity + 1)}>+</button>
                                            </div>
                                        )}
                                    </div>

                                    <strong className="cart-line__total">{formatPrice(line.lineTotal)}</strong>
                                    <button type="button" className="cart-line__remove" disabled={busy}
                                            onClick={() => run(line.cartItemId, () => remove(line.cartItemId))}>
                                        Xóa
                                    </button>
                                </li>
                            )
                        })}
                    </ul>

                    <aside className="cart__summary">
                        <h2>Tóm tắt</h2>
                        <p className="cart__row"><span>Số món</span><span>{cart.itemCount}</span></p>
                        <p className="cart__row cart__row--total"><span>Tổng tiền</span><strong>{formatPrice(cart.total)}</strong></p>
                        <button type="button" className="cart__checkout" disabled
                                title="Chức năng đặt hàng sẽ có ở bước tiếp theo">Đặt hàng (sắp có)</button>
                        <p className="cart__hint">Xe máy được tạm giữ {cart.holdMinutes} phút kể từ lúc thêm vào giỏ.</p>
                    </aside>
                </div>
            )}
        </section>
    )
}
