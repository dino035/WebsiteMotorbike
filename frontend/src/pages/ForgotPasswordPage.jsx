import { useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client.js'
import { apiError } from '../utils/apiError.js'
import './LoginPage.css'

/** Bước 1: nhập email. Backend luôn trả cùng một thông báo dù email có tài khoản hay không. */
export default function ForgotPasswordPage() {
    const [email, setEmail] = useState('')
    const [error, setError] = useState('')
    const [sentMessage, setSentMessage] = useState('')
    const [submitting, setSubmitting] = useState(false)

    async function handleSubmit(e) {
        e.preventDefault()
        setError('')
        setSubmitting(true)
        try {
            const res = await client.post('/auth/forgot-password', { email: email.trim() })
            setSentMessage(res.data.message)
        } catch (err) {
            setError(apiError(err, 'Không gửi được yêu cầu, vui lòng thử lại.'))
        } finally {
            setSubmitting(false)
        }
    }

    if (sentMessage) {
        return (
            <section className="login">
                <h1>Kiểm tra email của bạn</h1>
                <p className="login__success" role="status">{sentMessage}</p>
                <p className="login__switch">
                    Không thấy email? Kiểm tra thư mục spam, hoặc <button type="button" className="login__link-button"
                        onClick={() => setSentMessage('')}>gửi lại</button>.
                </p>
                <p className="login__switch"><Link to="/login">Quay lại đăng nhập</Link></p>
            </section>
        )
    }

    return (
        <section className="login">
            <h1>Quên mật khẩu</h1>
            <p className="login__hint">Nhập email đã đăng ký, chúng tôi sẽ gửi liên kết để đặt lại mật khẩu.</p>
            <form onSubmit={handleSubmit} className="login__form">
                <label>
                    Email
                    <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                           autoComplete="username" required />
                </label>
                {error && <p className="login__error" role="alert">{error}</p>}
                <button type="submit" disabled={submitting}>{submitting ? 'Đang gửi...' : 'Gửi liên kết'}</button>
            </form>
            <p className="login__switch"><Link to="/login">Quay lại đăng nhập</Link></p>
        </section>
    )
}
