import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import client from '../api/client.js'
import { apiError } from '../utils/apiError.js'
import './OrderPages.css'

/**
 * VNPay đưa trình duyệt của khách quay về đây kèm các tham số vnp_* đã ký. Trang gửi nguyên chúng cho backend để
 * kiểm tra chữ ký và ghi nhận kết quả (không đòi đăng nhập: kết quả được tin nhờ chữ ký). Backend chạy lặp lại vẫn
 * đúng, nên tải lại trang hay IPN đã ghi nhận trước đó đều hiện đúng kết quả.
 */
export default function VnpayReturnPage() {
    const { search } = useLocation()
    const hasParams = search.includes('vnp_')
    const [result, setResult] = useState({ key: null, data: null, error: '' })

    useEffect(() => {
        if (!hasParams) return undefined
        let ignore = false
        client.get(`/payments/vnpay/return${search}`)
            .then((res) => { if (!ignore) setResult({ key: search, data: res.data, error: '' }) })
            .catch((err) => {
                if (!ignore) setResult({ key: search, data: null, error: apiError(err, 'Không xác nhận được kết quả thanh toán.') })
            })
        return () => { ignore = true }
    }, [search, hasParams])

    if (!hasParams) {
        return (
            <section className="vnpay-result">
                <h1>Kết quả thanh toán</h1>
                <p>Trang này chỉ hiện sau khi bạn thanh toán trên cổng VNPay.</p>
                <div className="vnpay-result__actions">
                    <Link to="/orders" className="vnpay-result__link">Đơn hàng của tôi</Link>
                </div>
            </section>
        )
    }
    if (result.key !== search) {
        return <section className="vnpay-result"><p>Đang xác nhận kết quả thanh toán...</p></section>
    }
    if (result.error) {
        return (
            <section className="vnpay-result vnpay-result--fail">
                <h1>Không xác nhận được thanh toán</h1>
                <p>{result.error}</p>
                <p>Nếu tiền đã bị trừ, vui lòng liên hệ cửa hàng kèm mã đơn hàng.</p>
                <div className="vnpay-result__actions">
                    <Link to="/orders" className="vnpay-result__link">Đơn hàng của tôi</Link>
                </div>
            </section>
        )
    }

    const r = result.data
    // paid: lần trả tiền này đã thành công. Các kết quả "cần cửa hàng xử lý" (đơn đã hủy, trả trùng) cũng có paid = true.
    const needsStaff = r.outcome === 'PAID_ORDER_CANCELLED' || r.outcome === 'PAID_DUPLICATE'
    const tone = needsStaff ? 'warn' : r.paid ? 'ok' : 'fail'
    const title = needsStaff ? 'Đã nhận tiền nhưng cần cửa hàng xử lý'
        : r.paid ? 'Thanh toán thành công' : 'Thanh toán không thành công'

    return (
        <section className={`vnpay-result vnpay-result--${tone}`}>
            <h1>{title}</h1>
            <p>{r.message}</p>
            {r.orderCode && <p>Đơn hàng <strong>#{r.orderCode}</strong></p>}
            {!r.paid && r.reason && <p>Lý do: {r.reason}</p>}
            <div className="vnpay-result__actions">
                {r.orderId && (
                    <Link to={`/orders/${r.orderId}`}
                          className={`vnpay-result__link${r.paid ? ' vnpay-result__link--primary' : ''}`}>
                        {!r.paid && r.outcome === 'FAILED' ? 'Về đơn hàng để thử lại' : 'Xem đơn hàng'}
                    </Link>
                )}
                <Link to="/orders" className="vnpay-result__link">Đơn hàng của tôi</Link>
            </div>
        </section>
    )
}
