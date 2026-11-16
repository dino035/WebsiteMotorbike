import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import client from '../api/client.js'
import { apiError } from '../utils/apiError.js'
import './LoginPage.css'

/**
 * Bước 2: khách mở liên kết trong email (/reset-password?token=...). Trang kiểm tra mã trước để báo
 * liên kết hết hạn ngay, không bắt khách điền biểu mẫu rồi mới biết.
 */
export default function ResetPasswordPage() {
    const [params] = useSearchParams()
    const token = params.get('token') || ''

    // Kết quả kiểm tra gắn với token đã kiểm: đổi token thì tự coi là đang kiểm tra lại.
    const [check, setCheck] = useState({ token: null, valid: false, failed: false })
    const [form, setForm] = useState({ password: '', confirm: '' })
    const [fieldErrors, setFieldErrors] = useState({})
    const [error, setError] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [done, setDone] = useState('')

    useEffect(() => {
        if (!token) return
        let ignore = false
        client.post('/auth/reset-password/validate', { token })
            .then((res) => { if (!ignore) setCheck({ token, valid: res.data.valid, failed: false }) })
            .catch(() => { if (!ignore) setCheck({ token, valid: false, failed: true }) })
        return () => { ignore = true }
    }, [token])

    const setField = (name) => (e) => setForm((f) => ({ ...f, [name]: e.target.value }))

    async function handleSubmit(e) {
        e.preventDefault()
        setError('')
        const errors = {}
        if (form.password.length < 8) errors.password = 'Mật khẩu phải từ 8 đến 72 ký tự.'
        if (form.confirm !== form.password) errors.confirm = 'Mật khẩu nhập lại không khớp.'
        setFieldErrors(errors)
        if (Object.keys(errors).length > 0) return

        setSubmitting(true)
        try {
            const res = await client.post('/auth/reset-password', {
                token, password: form.password, confirmPassword: form.confirm,
            })
            setDone(res.data.message)
        } catch (err) {
            setFieldErrors(err.response?.data?.errors ?? {})
            setError(apiError(err, 'Không đặt lại được mật khẩu.'))
            setSubmitting(false)
        }
    }

    if (done) {
        return (
            <section className="login">
                <h1>Đặt lại mật khẩu</h1>
                <p className="login__success" role="status">{done}</p>
                <Link to="/login" className="login__cta">Đăng nhập</Link>
            </section>
        )
    }

    if (!token) return <InvalidLink text="Liên kết không hợp lệ: thiếu mã đặt lại mật khẩu." />
    if (check.token !== token) return <p className="login__hint">Đang kiểm tra liên kết...</p>
    if (check.failed) return <InvalidLink text="Không kiểm tra được liên kết. Kiểm tra backend đã chạy chưa." retry />
    if (!check.valid) return <InvalidLink text="Liên kết đã hết hạn hoặc đã được dùng. Vui lòng yêu cầu liên kết mới." />

    return (
        <section className="login">
            <h1>Đặt lại mật khẩu</h1>
            <form onSubmit={handleSubmit} className="login__form" noValidate>
                <label>
                    Mật khẩu mới (từ 8 ký tự)
                    <input type="password" value={form.password} onChange={setField('password')}
                           autoComplete="new-password" required />
                    {fieldErrors.password && <span className="login__field-error">{fieldErrors.password}</span>}
                </label>
                <label>
                    Nhập lại mật khẩu mới
                    <input type="password" value={form.confirm} onChange={setField('confirm')}
                           autoComplete="new-password" required />
                    {fieldErrors.confirm && <span className="login__field-error">{fieldErrors.confirm}</span>}
                </label>
                {error && Object.keys(fieldErrors).length === 0 && <p className="login__error" role="alert">{error}</p>}
                <button type="submit" disabled={submitting}>{submitting ? 'Đang lưu...' : 'Đặt lại mật khẩu'}</button>
            </form>
        </section>
    )
}

function InvalidLink({ text, retry = false }) {
    return (
        <section className="login">
            <h1>Đặt lại mật khẩu</h1>
            <p className="login__error" role="alert">{text}</p>
            <p className="login__switch">
                {retry
                    ? <button type="button" className="login__link-button" onClick={() => window.location.reload()}>Thử lại</button>
                    : <Link to="/forgot-password">Yêu cầu liên kết mới</Link>}
            </p>
        </section>
    )
}
