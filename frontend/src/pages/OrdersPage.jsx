import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client.js'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime, formatPrice } from '../utils/format.js'
import './OrderPages.css'

/** Đơn hàng của tôi: mới nhất trước, bấm vào để xem chi tiết. */
export default function OrdersPage() {
    const { logout } = useAuth()
    const [orders, setOrders] = useState(null)
    const [error, setError] = useState('')

    useEffect(() => {
        let ignore = false
        client.get('/orders')
            .then((res) => { if (!ignore) setOrders(res.data) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setError(apiError(err, 'Không tải được danh sách đơn hàng.'))
            })
        return () => { ignore = true }
    }, [logout])

    if (error) return <p className="orders__message orders__message--error">{error}</p>
    if (!orders) return <p className="orders__message">Đang tải đơn hàng...</p>

    return (
        <section className="orders">
            <h1>Đơn hàng của tôi</h1>
            {orders.length === 0 ? (
                <div className="orders__empty">
                    <p>Bạn chưa có đơn hàng nào.</p>
                    <Link to="/products" className="orders__link-button">Xem sản phẩm</Link>
                </div>
            ) : (
                <ul className="orders__list">
                    {orders.map((o) => (
                        <li key={o.orderId}>
                            <Link to={`/orders/${o.orderId}`} className="order-row">
                                <div>
                                    <strong>#{o.shortCode}</strong>
                                    <span className="order-row__date">{formatDateTime(o.orderDate)}</span>
                                    <span className="order-row__meta">{o.itemCount} món · {o.paymentMethodLabel}</span>
                                </div>
                                <div className="order-row__right">
                                    <span className={`status-badge status-badge--${o.status.toLowerCase()}`}>{o.statusLabel}</span>
                                    <span className={`pay-badge pay-badge--${(o.paymentStatus || '').toLowerCase()}`}>{o.paymentStatusLabel}</span>
                                    <strong className="order-row__total">{formatPrice(o.totalAmount)}</strong>
                                </div>
                            </Link>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    )
}
