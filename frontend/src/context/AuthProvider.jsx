import { useCallback, useMemo, useState } from 'react'
import client, { TOKEN_KEY } from '../api/client.js'
import { AuthContext } from './auth-context.js'

const USER_KEY = 'motoparts.user'

function readStored() {
    try {
        const token = localStorage.getItem(TOKEN_KEY)
        const user = JSON.parse(localStorage.getItem(USER_KEY) || 'null')
        return token && user ? { token, user } : { token: null, user: null }
    } catch {
        return { token: null, user: null }
    }
}

export default function AuthProvider({ children }) {
    const [auth, setAuth] = useState(readStored)

    // Đăng nhập và đăng ký đều trả {token, user}: lưu lại để các lần gọi API sau tự gắn token.
    const startSession = useCallback((data) => {
        try {
            localStorage.setItem(TOKEN_KEY, data.token)
            localStorage.setItem(USER_KEY, JSON.stringify(data.user))
        } catch {
            // không lưu được thì chỉ giữ trong bộ nhớ, tải lại trang sẽ phải đăng nhập lại
        }
        setAuth({ token: data.token, user: data.user })
        return data.user
    }, [])

    const login = useCallback(
        (email, password) => client.post('/auth/login', { email, password }).then((res) => startSession(res.data)),
        [startSession],
    )

    // Đăng ký xong được đăng nhập luôn.
    const register = useCallback(
        (email, password, phone) => client.post('/auth/register', { email, password, phone })
            .then((res) => startSession(res.data)),
        [startSession],
    )

    const logout = useCallback(() => {
        try {
            localStorage.removeItem(TOKEN_KEY)
            localStorage.removeItem(USER_KEY)
        } catch {
            // bỏ qua
        }
        setAuth({ token: null, user: null })
    }, [])

    const value = useMemo(() => ({ ...auth, login, register, logout }), [auth, login, register, logout])
    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
