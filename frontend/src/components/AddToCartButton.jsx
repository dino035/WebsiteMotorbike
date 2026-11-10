import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import useAuth from '../hooks/useAuth.js'
import useCart from '../hooks/useCart.js'
import { apiError } from '../utils/apiError.js'
import './AddToCartButton.css'

/** Nút "Thêm vào giỏ" cho trang chi tiết. Xe máy chỉ 1 chiếc; phụ tùng chọn được số lượng. */
export default function AddToCartButton({ product }) {
    const { token } = useAuth()
    const { add } = useCart()
    const navigate = useNavigate()
    const location = useLocation()

    const [quantity, setQuantity] = useState(1)
    const [busy, setBusy] = useState(false)
    const [message, setMessage] = useState({ type: '', text: '' })

    const available = product.status === 'AVAILABLE'
    const isMotorbike = Boolean(product.motorbike)

    async function handleAdd() {
        if (!token) {
            navigate('/login', { state: { from: location.pathname } })
            return
        }
        setBusy(true)
        setMessage({ type: '', text: '' })
        try {
            await add(product.productId, isMotorbike ? 1 : quantity)
            setMessage({ type: 'ok', text: isMotorbike ? 'Đã thêm xe vào giỏ và tạm giữ cho bạn.' : 'Đã thêm vào giỏ hàng.' })
        } catch (err) {
            setMessage({ type: 'error', text: apiError(err, 'Không thêm được vào giỏ hàng.') })
        } finally {
            setBusy(false)
        }
    }

    function changeQuantity(value) {
        const n = Number.parseInt(value, 10)
        setQuantity(Number.isNaN(n) ? 1 : Math.min(99, Math.max(1, n)))
    }

    return (
        <div className="add-to-cart">
            {!isMotorbike && available && (
                <label className="add-to-cart__qty">
                    Số lượng
                    <input type="number" min="1" max="99" value={quantity}
                           onChange={(e) => changeQuantity(e.target.value)} />
                </label>
            )}
            <button type="button" className="add-to-cart__button" onClick={handleAdd} disabled={!available || busy}>
                {!available ? 'Đã hết hàng' : busy ? 'Đang thêm...' : 'Thêm vào giỏ hàng'}
            </button>
            {message.text && (
                <p className={`add-to-cart__msg add-to-cart__msg--${message.type}`} role="status">
                    {message.text}{' '}
                    {message.type === 'ok' && <Link to="/cart">Xem giỏ hàng</Link>}
                </p>
            )}
        </div>
    )
}
