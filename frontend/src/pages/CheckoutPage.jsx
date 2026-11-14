import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import client from '../api/client.js'
import useAuth from '../hooks/useAuth.js'
import useCart from '../hooks/useCart.js'
import { apiError } from '../utils/apiError.js'
import { formatPrice, isoDateFromToday } from '../utils/format.js'
import './CheckoutPage.css'

const NEW_ADDRESS = 'new'   // giá trị "nhập địa chỉ khác" trong danh sách chọn địa chỉ

/** Trang đặt hàng: tải giỏ, sổ địa chỉ và quy tắc lắp đặt, rồi giao cho form. */
export default function CheckoutPage() {
    const { logout } = useAuth()
    const { cart, refresh } = useCart()
    const [extra, setExtra] = useState(null)   // { addresses, policy }
    const [error, setError] = useState('')
    const [loadedCart, setLoadedCart] = useState(false)

    useEffect(() => {
        let ignore = false
        Promise.all([refresh(), client.get('/addresses'), client.get('/orders/policy')])
            .then(([, addresses, policy]) => {
                if (ignore) return
                setExtra({ addresses: addresses.data, policy: policy.data })
                setLoadedCart(true)
            })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setError(apiError(err, 'Không tải được trang đặt hàng.'))
            })
        return () => { ignore = true }
    }, [refresh, logout])

    if (error) return <p className="checkout__message checkout__message--error">{error}</p>
    if (!extra || !loadedCart || !cart) return <p className="checkout__message">Đang tải...</p>

    if (cart.lines.length === 0) {
        return (
            <section className="checkout">
                <h1>Đặt hàng</h1>
                <p className="checkout__message">Giỏ hàng của bạn đang trống.</p>
                <Link to="/products" className="checkout__link-button">Xem sản phẩm</Link>
            </section>
        )
    }
    return <CheckoutForm cart={cart} addresses={extra.addresses} policy={extra.policy} />
}

function CheckoutForm({ cart, addresses, policy }) {
    const { refresh } = useCart()
    const navigate = useNavigate()
    const hasSparePart = cart.lines.some((l) => !l.motorbike)
    const defaultAddress = addresses.find((a) => a.defaultAddress) ?? addresses[0] ?? null

    const [deliveryMethod, setDeliveryMethod] = useState('DELIVERY')
    const [addressChoice, setAddressChoice] = useState(defaultAddress ? defaultAddress.addressId : NEW_ADDRESS)
    const [receiverName, setReceiverName] = useState(defaultAddress?.receiverName ?? '')
    const [receiverPhone, setReceiverPhone] = useState(defaultAddress?.receiverPhone ?? '')
    const [shippingAddress, setShippingAddress] = useState('')   // chỉ dùng khi chọn "địa chỉ khác"
    const [installDate, setInstallDate] = useState('')
    const [accepted, setAccepted] = useState(false)
    const [error, setError] = useState('')
    const [submitting, setSubmitting] = useState(false)

    const labor = deliveryMethod === 'INSTALL' ? policy.laborFee : 0
    const total = cart.total + labor

    function chooseAddress(value) {
        setAddressChoice(value)
        const a = addresses.find((x) => x.addressId === value)
        if (a) {   // chọn địa chỉ trong sổ thì điền sẵn người nhận (vẫn sửa được)
            setReceiverName(a.receiverName)
            setReceiverPhone(a.receiverPhone)
        }
    }

    async function handleSubmit(e) {
        e.preventDefault()
        setError('')
        const saved = addresses.find((a) => a.addressId === addressChoice)
        const address = deliveryMethod === 'DELIVERY' ? (saved ? saved.fullAddress : shippingAddress.trim()) : null
        if (deliveryMethod === 'DELIVERY' && !address) {
            setError('Vui lòng chọn hoặc nhập địa chỉ giao hàng.')
            return
        }
        if (deliveryMethod === 'INSTALL' && !installDate) {
            setError('Vui lòng chọn ngày hẹn lắp đặt.')
            return
        }
        if (!accepted) {
            setError('Vui lòng xác nhận chính sách đổi trả để tiếp tục.')
            return
        }

        setSubmitting(true)
        try {
            const res = await client.post('/orders', {
                deliveryMethod,
                receiverName: receiverName.trim(),
                receiverPhone: receiverPhone.trim(),
                shippingAddress: address,
                paymentMethod: 'COD',
                returnPolicyAccepted: accepted,
                installDate: deliveryMethod === 'INSTALL' ? installDate : null,
            })
            refresh().catch(() => {})   // giỏ đã trống: cập nhật số món trên menu
            navigate(`/orders/${res.data.orderId}`, { replace: true, state: { placed: true } })
        } catch (err) {
            setError(apiError(err, 'Không đặt hàng được, vui lòng thử lại.'))
            setSubmitting(false)
            if (err.response?.status === 409) refresh().catch(() => {})   // giỏ có thể đã đổi: tải lại cho đúng
        }
    }

    return (
        <section className="checkout">
            <h1>Đặt hàng</h1>
            <form className="checkout__layout" onSubmit={handleSubmit} noValidate>
                <div className="checkout__main">
                    <fieldset className="checkout__box">
                        <legend>Hình thức nhận hàng</legend>
                        <label className="checkout__option">
                            <input type="radio" name="delivery" checked={deliveryMethod === 'DELIVERY'}
                                   onChange={() => setDeliveryMethod('DELIVERY')} />
                            Giao hàng tận nơi
                        </label>
                        <label className="checkout__option">
                            <input type="radio" name="delivery" checked={deliveryMethod === 'PICKUP'}
                                   onChange={() => setDeliveryMethod('PICKUP')} />
                            Nhận tại cửa hàng
                        </label>
                        <label className={hasSparePart ? 'checkout__option' : 'checkout__option checkout__option--off'}>
                            <input type="radio" name="delivery" disabled={!hasSparePart}
                                   checked={deliveryMethod === 'INSTALL'} onChange={() => setDeliveryMethod('INSTALL')} />
                            Lắp đặt tại cửa hàng (+{formatPrice(policy.laborFee)} phí công)
                            {!hasSparePart && <span className="checkout__note"> — chỉ áp dụng khi giỏ có phụ tùng</span>}
                        </label>

                        {deliveryMethod === 'INSTALL' && (
                            <label className="checkout__field">
                                Ngày hẹn lắp đặt (từ ngày mai, tối đa {policy.installMaxDaysAhead} ngày tới;
                                mỗi ngày nhận tối đa {policy.installMaxPerDay} lượt)
                                <input type="date" value={installDate} onChange={(e) => setInstallDate(e.target.value)}
                                       min={isoDateFromToday(1)} max={isoDateFromToday(policy.installMaxDaysAhead)} />
                            </label>
                        )}
                    </fieldset>

                    <fieldset className="checkout__box">
                        <legend>Người nhận</legend>
                        {deliveryMethod === 'DELIVERY' && (
                            <div className="checkout__addresses">
                                {addresses.map((a) => (
                                    <label key={a.addressId} className="checkout__option checkout__option--address">
                                        <input type="radio" name="address" checked={addressChoice === a.addressId}
                                               onChange={() => chooseAddress(a.addressId)} />
                                        <span>
                                            <strong>{a.receiverName}</strong> · {a.receiverPhone}
                                            {a.defaultAddress && <em className="checkout__badge">Mặc định</em>}
                                            <br />{a.fullAddress}
                                        </span>
                                    </label>
                                ))}
                                <label className="checkout__option">
                                    <input type="radio" name="address" checked={addressChoice === NEW_ADDRESS}
                                           onChange={() => chooseAddress(NEW_ADDRESS)} />
                                    Nhập địa chỉ khác
                                </label>
                                {addresses.length === 0 && (
                                    <p className="checkout__note">Bạn chưa lưu địa chỉ nào. <Link to="/addresses">Thêm vào sổ địa chỉ</Link> để lần sau chọn nhanh.</p>
                                )}
                                {addressChoice === NEW_ADDRESS && (
                                    <label className="checkout__field">
                                        Địa chỉ giao hàng
                                        <textarea rows="2" value={shippingAddress} maxLength={500}
                                                  placeholder="Số nhà, đường, quận/huyện, tỉnh/thành phố"
                                                  onChange={(e) => setShippingAddress(e.target.value)} />
                                    </label>
                                )}
                            </div>
                        )}
                        <label className="checkout__field">
                            Họ tên người nhận
                            <input type="text" value={receiverName} maxLength={100}
                                   onChange={(e) => setReceiverName(e.target.value)} autoComplete="name" required />
                        </label>
                        <label className="checkout__field">
                            Số điện thoại
                            <input type="tel" value={receiverPhone} onChange={(e) => setReceiverPhone(e.target.value)}
                                   autoComplete="tel" required />
                        </label>
                    </fieldset>

                    <fieldset className="checkout__box">
                        <legend>Thanh toán</legend>
                        <label className="checkout__option">
                            <input type="radio" name="payment" checked readOnly />
                            Thanh toán khi nhận hàng (COD)
                        </label>
                        <label className="checkout__option checkout__option--off">
                            <input type="radio" name="payment" disabled />
                            VNPay <span className="checkout__note">— sắp có</span>
                        </label>
                    </fieldset>

                    <label className="checkout__policy">
                        <input type="checkbox" checked={accepted} onChange={(e) => setAccepted(e.target.checked)} />
                        <span>Tôi đã đọc và đồng ý với chính sách miễn đổi trả (cửa hàng chỉ kinh doanh xe đã qua sử dụng
                            và phụ tùng; hàng đã bán không đổi trả).</span>
                    </label>
                </div>

                <aside className="checkout__summary">
                    <h2>Đơn hàng ({cart.itemCount} món)</h2>
                    <ul className="checkout__lines">
                        {cart.lines.map((l) => (
                            <li key={l.cartItemId}>
                                <span>{l.name} × {l.quantity}</span>
                                <span>{formatPrice(l.lineTotal)}</span>
                            </li>
                        ))}
                    </ul>
                    <p className="checkout__row"><span>Tạm tính</span><span>{formatPrice(cart.total)}</span></p>
                    {labor > 0 && <p className="checkout__row"><span>Phí công lắp đặt</span><span>{formatPrice(labor)}</span></p>}
                    <p className="checkout__row checkout__row--total"><span>Tổng cộng</span><strong>{formatPrice(total)}</strong></p>

                    {error && <p className="checkout__error" role="alert">{error}</p>}
                    <button type="submit" className="checkout__submit" disabled={submitting}>
                        {submitting ? 'Đang đặt hàng...' : 'Đặt hàng'}
                    </button>
                    <p className="checkout__note"><Link to="/cart">← Quay lại giỏ hàng</Link></p>
                </aside>
            </form>
        </section>
    )
}
