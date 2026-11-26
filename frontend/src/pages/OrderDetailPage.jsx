import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import client from '../api/client.js'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDate, formatDateTime, formatPrice } from '../utils/format.js'
import { totalIncludesLaborFee } from '../utils/orderTotal.js'
import './OrderPages.css'

/** Chi tiết một đơn: dòng hàng (giá chụp lúc đặt), người nhận, thanh toán, lịch lắp đặt. */
export default function OrderDetailPage() {
    const { id } = useParams()
    const { logout } = useAuth()
    const location = useLocation()
    const justPlaced = location.state?.placed === true
    const [result, setResult] = useState({ id: null, order: null, error: '' })
    const [busy, setBusy] = useState(false)
    const [actionError, setActionError] = useState(location.state?.payError ?? '')
    const [holdMinutes, setHoldMinutes] = useState(null)   // số phút để trả tiền VNPay trước khi đơn tự hủy

    useEffect(() => {
        client.get('/orders/policy')
            .then((res) => setHoldMinutes(res.data.vnpayHoldMinutes))
            .catch(() => {})   // chỉ là lời nhắc, không có cũng không sao
    }, [])

    useEffect(() => {
        let ignore = false
        client.get(`/orders/${encodeURIComponent(id)}`)
            .then((res) => { if (!ignore) setResult({ id, order: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) { logout(); return }
                const message = err.response?.status === 404
                    ? 'Không tìm thấy đơn hàng này.'
                    : apiError(err, 'Không tải được đơn hàng.')
                setResult({ id, order: null, error: message })
            })
        return () => { ignore = true }
    }, [id, logout])

    /** Bắt đầu (hoặc thử lại) một lần trả tiền VNPay rồi chuyển khách sang cổng thanh toán. */
    async function payOnline() {
        setBusy(true)
        setActionError('')
        try {
            const res = await client.post(`/orders/${encodeURIComponent(id)}/vnpay`)
            window.location.assign(res.data.paymentUrl)   // nút vẫn khóa trong lúc chuyển trang
        } catch (err) {
            if (err.response?.status === 401) { logout(); return }
            setActionError(apiError(err, 'Không mở được cổng thanh toán, vui lòng thử lại.'))
            setBusy(false)
        }
    }

    /** Thanh toán trực tuyến lỗi thì khách được đổi sang thanh toán khi nhận hàng (SRS 3.10). */
    async function switchToCod() {
        if (!window.confirm('Chuyển sang thanh toán khi nhận hàng (COD)? Bạn sẽ trả tiền mặt khi nhận hàng.')) return
        setBusy(true)
        setActionError('')
        try {
            const res = await client.post(`/orders/${encodeURIComponent(id)}/pay-cod`)
            setResult({ id, order: res.data, error: '' })
        } catch (err) {
            if (err.response?.status === 401) { logout(); return }
            setActionError(apiError(err, 'Không đổi được phương thức thanh toán, vui lòng thử lại.'))
        } finally {
            setBusy(false)
        }
    }

    const loading = result.id !== id
    const o = result.order

    return (
        <section className="order-detail">
            <Link to="/orders" className="order-detail__back">← Đơn hàng của tôi</Link>

            {loading && <p className="orders__message">Đang tải...</p>}
            {!loading && result.error && <p className="orders__message orders__message--error">{result.error}</p>}

            {!loading && o && (
                <>
                    {justPlaced && (
                        <p className="order-detail__success" role="status">
                            Đặt hàng thành công! Mã đơn hàng: <strong>#{o.shortCode}</strong>. Cửa hàng sẽ sớm liên hệ xác nhận.
                        </p>
                    )}
                    <div className="order-detail__head">
                        <h1>Đơn hàng #{o.shortCode}</h1>
                        <span className={`status-badge status-badge--${o.status.toLowerCase()}`}>{o.statusLabel}</span>
                    </div>
                    <p className="order-detail__date">Đặt lúc {formatDateTime(o.orderDate)}</p>
                    {o.status === 'COMPLETED' && (
                        <p className="order-detail__date"><Link to="/warranty">Sản phẩm bị lỗi? Gửi yêu cầu bảo hành</Link></p>
                    )}

                    <div className="order-detail__grid">
                        <div className="order-detail__box">
                            <h2>Sản phẩm</h2>
                            <table className="order-detail__table">
                                <thead>
                                    <tr><th>Sản phẩm</th><th>Đơn giá</th><th>SL</th><th>Thành tiền</th></tr>
                                </thead>
                                <tbody>
                                    {o.items.map((i) => (
                                        <tr key={i.productId}>
                                            <td><Link to={`/products/${i.productId}`}>{i.productName}</Link></td>
                                            <td>{formatPrice(i.unitPrice)}</td>
                                            <td>{i.quantity}</td>
                                            <td>{formatPrice(i.lineTotal)}</td>
                                        </tr>
                                    ))}
                                    {totalIncludesLaborFee(o) && (
                                        <tr>
                                            <td colSpan="3">Phí công lắp đặt</td>
                                            <td>{formatPrice(o.installation.laborFee)}</td>
                                        </tr>
                                    )}
                                </tbody>
                                <tfoot>
                                    <tr><td colSpan="3">Tổng cộng</td><td><strong>{formatPrice(o.totalAmount)}</strong></td></tr>
                                </tfoot>
                            </table>
                        </div>

                        <div className="order-detail__side">
                            <div className="order-detail__box">
                                <h2>Nhận hàng</h2>
                                <p><strong>{o.deliveryMethodLabel}</strong></p>
                                <p>{o.receiverName} · {o.receiverPhone}</p>
                                {o.shippingAddress && <p>{o.shippingAddress}</p>}
                                {o.installation && (
                                    <p>
                                        Lịch lắp đặt: <strong>{formatDate(o.installation.preferredDate)}</strong>
                                        {' '}({o.installation.statusLabel})
                                    </p>
                                )}
                            </div>
                            {o.payment && (
                                <div className="order-detail__box">
                                    <h2>Thanh toán</h2>
                                    <p>{o.payment.methodLabel}</p>
                                    <p>
                                        <span className={`pay-badge pay-badge--${o.payment.status.toLowerCase()}`}>{o.payment.statusLabel}</span>
                                        {o.payment.paidAt && <> lúc {formatDateTime(o.payment.paidAt)}</>}
                                    </p>
                                    {o.payment.canPayOnline && (
                                        <div className="order-detail__pay">
                                            <button type="button" className="order-detail__btn order-detail__btn--primary"
                                                    disabled={busy} onClick={payOnline}>
                                                {o.payment.status === 'FAILED' ? 'Thanh toán lại bằng VNPay' : 'Thanh toán bằng VNPay'}
                                            </button>
                                            <button type="button" className="order-detail__btn" disabled={busy} onClick={switchToCod}>
                                                Chuyển sang thanh toán khi nhận hàng
                                            </button>
                                        </div>
                                    )}
                                    {o.payment.canPayOnline && holdMinutes && (
                                        <p className="order-detail__hint">
                                            Đơn chưa thanh toán trong {holdMinutes} phút kể từ lúc đặt sẽ tự hủy
                                            (nếu bạn vừa bấm thanh toán thì được chờ thêm cho tới khi liên kết hết hạn).
                                        </p>
                                    )}
                                    {actionError && <p className="order-detail__error" role="alert">{actionError}</p>}
                                </div>
                            )}
                        </div>
                    </div>
                </>
            )}
        </section>
    )
}
