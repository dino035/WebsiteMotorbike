import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import './LoginPage.css'

export default function LoginPage() {
    const { token, login } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()
    const from = location.state?.from || '/'

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [submitting, setSubmitting] = useState(false)

    if (token) return <Navigate to={from} replace />

    async function handleSubmit(e) {
        e.preventDefault()
        setError('')
        setSubmitting(true)
        try {
            await login(email.trim(), password)
            navigate(from, { replace: true })
        } catch (err) {
            setError(apiError(err, 'Email hoặc mật khẩu không đúng.'))
            setSubmitting(false)
        }
    }

    return (
        <section className="login">
            <h1>Đăng nhập</h1>
            {from === '/cart' && <p className="login__hint">Vui lòng đăng nhập để xem giỏ hàng.</p>}
            <form onSubmit={handleSubmit} className="login__form">
                <label>
                    Email
                    <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                           autoComplete="username" required />
                </label>
                <label>
                    Mật khẩu
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                           autoComplete="current-password" required />
                </label>
                {error && <p className="login__error" role="alert">{error}</p>}
                <button type="submit" disabled={submitting}>{submitting ? 'Đang đăng nhập...' : 'Đăng nhập'}</button>
            </form>
        </section>
    )
}
