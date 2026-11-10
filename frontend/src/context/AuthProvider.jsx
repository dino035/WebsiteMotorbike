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

    const login = useCallback(async (email, password) => {
        const res = await client.post('/auth/login', { email, password })
        try {
            localStorage.setItem(TOKEN_KEY, res.data.token)
            localStorage.setItem(USER_KEY, JSON.stringify(res.data.user))
        } catch {
            // không lưu được thì chỉ giữ trong bộ nhớ, tải lại trang sẽ phải đăng nhập lại
        }
        setAuth({ token: res.data.token, user: res.data.user })
        return res.data.user
    }, [])

    const logout = useCallback(() => {
        try {
            localStorage.removeItem(TOKEN_KEY)
            localStorage.removeItem(USER_KEY)
        } catch {
            // bỏ qua
        }
        setAuth({ token: null, user: null })
    }, [])

    const value = useMemo(() => ({ ...auth, login, logout }), [auth, login, logout])
    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
