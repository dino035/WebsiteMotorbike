import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import './LoginPage.css'

export default function RegisterPage() {
    const { token, register } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()
    const from = location.state?.from || '/'

    const [form, setForm] = useState({ email: '', phone: '', password: '', confirm: '' })
    const [fieldErrors, setFieldErrors] = useState({})   // lỗi theo từng ô, lấy từ backend hoặc kiểm tra tại chỗ
    const [error, setError] = useState('')
    const [submitting, setSubmitting] = useState(false)

    if (token) return <Navigate to={from} replace />

    const setField = (name) => (e) => setForm((f) => ({ ...f, [name]: e.target.value }))

    function validate() {
        const errors = {}
        if (form.password.length < 8) errors.password = 'Mật khẩu phải từ 8 đến 72 ký tự.'
        if (form.confirm !== form.password) errors.confirm = 'Mật khẩu nhập lại không khớp.'
        if (form.phone && !/^0\d{9}$/.test(form.phone.trim())) {
            errors.phone = 'Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.'
        }
        return errors
    }

    async function handleSubmit(e) {
        e.preventDefault()
        setError('')
        const errors = validate()
        setFieldErrors(errors)
        if (Object.keys(errors).length > 0) return

        setSubmitting(true)
        try {
            await register(form.email.trim(), form.password, form.phone.trim())
            navigate(from, { replace: true })
        } catch (err) {
            // 400: backend trả lỗi theo từng trường; 409: email đã có người dùng
            setFieldErrors(err.response?.data?.errors ?? {})
            setError(apiError(err, 'Không đăng ký được, vui lòng thử lại.'))
            setSubmitting(false)
        }
    }

    return (
        <section className="login">
            <h1>Đăng ký</h1>
            <form onSubmit={handleSubmit} className="login__form" noValidate>
                <label>
                    Email
                    <input type="email" value={form.email} onChange={setField('email')}
                           autoComplete="username" required />
                    {fieldErrors.email && <span className="login__field-error">{fieldErrors.email}</span>}
                </label>
                <label>
                    Số điện thoại (không bắt buộc)
                    <input type="tel" value={form.phone} onChange={setField('phone')}
                           autoComplete="tel" placeholder="0912345678" />
                    {fieldErrors.phone && <span className="login__field-error">{fieldErrors.phone}</span>}
                </label>
                <label>
                    Mật khẩu (từ 8 ký tự)
                    <input type="password" value={form.password} onChange={setField('password')}
                           autoComplete="new-password" required />
                    {fieldErrors.password && <span className="login__field-error">{fieldErrors.password}</span>}
                </label>
                <label>
                    Nhập lại mật khẩu
                    <input type="password" value={form.confirm} onChange={setField('confirm')}
                           autoComplete="new-password" required />
                    {fieldErrors.confirm && <span className="login__field-error">{fieldErrors.confirm}</span>}
                </label>
                {error && <p className="login__error" role="alert">{error}</p>}
                <button type="submit" disabled={submitting}>{submitting ? 'Đang đăng ký...' : 'Đăng ký'}</button>
            </form>
            <p className="login__switch">
                Đã có tài khoản? <Link to="/login" state={{ from }}>Đăng nhập</Link>
            </p>
        </section>
    )
}
