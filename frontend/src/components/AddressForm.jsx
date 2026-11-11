import { useState } from 'react'
import { apiError } from '../utils/apiError.js'

const FIELDS = [
    ['receiverName', 'Họ tên người nhận', 'name'],
    ['receiverPhone', 'Số điện thoại', 'tel'],
    ['houseNumber', 'Số nhà', 'address-line1'],
    ['street', 'Tên đường', 'address-line2'],
    ['district', 'Quận/huyện', 'address-level2'],
    ['city', 'Tỉnh/thành phố', 'address-level1'],
]

const EMPTY_ADDRESS = {
    receiverName: '', receiverPhone: '', houseNumber: '', street: '', district: '', city: '', makeDefault: false,
}

/**
 * Form dùng chung cho thêm và sửa địa chỉ. onSubmit(values) trả Promise; lỗi 400 của backend
 * ({"errors": {"street": "..."}}) được hiện dưới đúng ô, lỗi khác hiện ở đầu form.
 */
export default function AddressForm({ initial = EMPTY_ADDRESS, forceDefault = false, submitLabel, onSubmit, onCancel }) {
    const [values, setValues] = useState(initial)
    const [fieldErrors, setFieldErrors] = useState({})
    const [error, setError] = useState('')
    const [submitting, setSubmitting] = useState(false)

    const setField = (name) => (e) => setValues((v) => ({ ...v, [name]: e.target.value }))

    async function handleSubmit(e) {
        e.preventDefault()
        setError('')
        setFieldErrors({})
        setSubmitting(true)
        try {
            await onSubmit({ ...values, makeDefault: forceDefault || values.makeDefault })
        } catch (err) {
            const errors = err.response?.data?.errors ?? {}
            setFieldErrors(errors)
            // Lỗi theo ô đã hiện dưới từng ô; khung chung chỉ dành cho lỗi không thuộc ô nào (ví dụ số điện thoại sai dạng).
            setError(Object.keys(errors).length > 0 ? '' : apiError(err, 'Không lưu được địa chỉ.'))
            setSubmitting(false)
        }
    }

    return (
        <form className="address-form" onSubmit={handleSubmit} noValidate>
            {FIELDS.map(([name, label, autoComplete]) => (
                <label key={name} className={name === 'receiverName' || name === 'street' ? 'address-form__wide' : ''}>
                    {label}
                    <input type={name === 'receiverPhone' ? 'tel' : 'text'} value={values[name]}
                           onChange={setField(name)} autoComplete={autoComplete} required />
                    {fieldErrors[name] && <span className="address-form__error">{fieldErrors[name]}</span>}
                </label>
            ))}

            <label className="address-form__check address-form__wide">
                <input type="checkbox" checked={forceDefault || values.makeDefault} disabled={forceDefault}
                       onChange={(e) => setValues((v) => ({ ...v, makeDefault: e.target.checked }))} />
                {forceDefault ? 'Địa chỉ đầu tiên tự là địa chỉ mặc định' : 'Đặt làm địa chỉ mặc định'}
            </label>

            {error && <p className="address-form__alert address-form__wide" role="alert">{error}</p>}

            <div className="address-form__actions address-form__wide">
                <button type="submit" disabled={submitting}>{submitting ? 'Đang lưu...' : submitLabel}</button>
                <button type="button" className="address-form__cancel" onClick={onCancel} disabled={submitting}>Hủy</button>
            </div>
        </form>
    )
}
