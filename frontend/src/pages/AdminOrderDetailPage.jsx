import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDate, formatDateTime, formatPrice } from '../utils/format.js'
import './OrderPages.css'
import './AdminPages.css'

/** Admin: chi tiết một đơn, đổi trạng thái, xác nhận đã thu tiền COD. Mọi thao tác trả lại chi tiết mới nhất. */
export default function AdminOrderDetailPage() {
    const { id } = useParams()
    const { logout } = useAuth()
    const [result, setResult] = useState({ id: null, detail: null, error: '' })
    const [busy, setBusy] = useState(false)
    const [actionError, setActionError] = useState('')
    const [done, setDone] = useState('')

    useEffect(() => {
        let ignore = false
        client.get(`/admin/orders/${encodeURIComponent(id)}`)
            .then((res) => { if (!ignore) setResult({ id, detail: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) { logout(); return }
                const message = err.response?.status === 404 ? 'Không tìm thấy đơn hàng này.' : apiError(err, 'Không tải được đơn hàng.')
                setResult({ id, detail: null, error: message })
            })
        return () => { ignore = true }
    }, [id, logout])

    async function act(request, successText) {
        setBusy(true)
        setActionError('')
        setDone('')
        try {
            const res = await request()
            setResult({ id, detail: res.data, error: '' })
            setDone(successText)
        } catch (err) {
            if (err.response?.status === 401) logout()
            else setActionError(apiError(err))
        } finally {
            setBusy(false)
        }
    }

    function changeStatus(next) {
        if (next.status === 'CANCELLED'
            && !window.confirm('Hủy đơn này? Xe trong đơn sẽ được trả về bán tiếp và lịch lắp đặt (nếu có) sẽ bị hủy.')) {
            return
        }
        act(() => client.post(`/admin/orders/${encodeURIComponent(id)}/status`, { status: next.status }),
            `Đã chuyển đơn sang "${next.label}".`)
    }

    function confirmPayment() {
        act(() => client.post(`/admin/orders/${encodeURIComponent(id)}/confirm-payment`), 'Đã xác nhận thu tiền.')
    }

    const loading = result.id !== id
    const detail = result.detail
    const o = detail?.order

    return (
        <section className="admin">
            <h1>Quản trị</h1>
            <AdminNav />
            <Link to="/admin/orders" className="order-detail__back">← Danh sách đơn hàng</Link>

            {loading && <p className="admin__message">Đang tải...</p>}
            {!loading && result.error && <p className="admin__message admin__message--error">{result.error}</p>}

            {!loading && o && (
                <>
                    <div className="order-detail__head">
                        <h2 className="admin__title">Đơn hàng #{o.shortCode}</h2>
                        <span className={`status-badge status-badge--${o.status.toLowerCase()}`}>{o.statusLabel}</span>
                    </div>
                    <p className="order-detail__date">Đặt lúc {formatDateTime(o.orderDate)}</p>

                    {done && <p className="order-detail__success" role="status">{done}</p>}
                    {actionError && <p className="admin__message admin__message--error" role="alert">{actionError}</p>}

                    <div className="admin__actions">
                        {detail.canConfirmPayment && (
                            <button type="button" className="admin__btn admin__btn--primary" disabled={busy} onClick={confirmPayment}>
                                Xác nhận đã thu tiền (COD)
                            </button>
                        )}
                        {detail.nextStatuses.map((s) => (
                            <button key={s.status} type="button" disabled={busy} onClick={() => changeStatus(s)}
                                    className={s.status === 'CANCELLED' ? 'admin__btn admin__btn--danger' : 'admin__btn'}>
                                {s.status === 'CANCELLED' ? 'Hủy đơn' : `Chuyển sang: ${s.label}`}
                            </button>
                        ))}
                        {detail.nextStatuses.length === 0 && <span className="admin__sub">Đơn ở trạng thái cuối, không đổi được nữa.</span>}
                    </div>
                    {detail.nextStatuses.some((s) => s.status === 'COMPLETED') && o.payment?.status !== 'PAID' && (
                        <p className="admin__hint">Cần xác nhận đã thu tiền trước khi hoàn thành đơn.</p>
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
                                            <td>{i.productName}</td>
                                            <td>{formatPrice(i.unitPrice)}</td>
                                            <td>{i.quantity}</td>
                                            <td>{formatPrice(i.lineTotal)}</td>
                                        </tr>
                                    ))}
                                    {o.installation && (
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
                                <h2>Khách hàng</h2>
                                <p>{detail.customer.email}</p>
                                {detail.customer.phone && <p>{detail.customer.phone}</p>}
                            </div>
                            <div className="order-detail__box">
                                <h2>Nhận hàng</h2>
                                <p><strong>{o.deliveryMethodLabel}</strong></p>
                                <p>{o.receiverName} · {o.receiverPhone}</p>
                                {o.shippingAddress && <p>{o.shippingAddress}</p>}
                                {o.installation && (
                                    <p>
                                        Lịch lắp đặt: <strong>{formatDate(o.installation.preferredDate)}</strong> ({o.installation.statusLabel}){' '}
                                        <Link to="/admin/installations">Quản lý lịch</Link>
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
                                </div>
                            )}
                        </div>
                    </div>
                </>
            )}
        </section>
    )
}
