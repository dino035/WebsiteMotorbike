import { Navigate, useLocation } from 'react-router-dom'
import useAuth from '../hooks/useAuth.js'

/** Trang cần đăng nhập: chưa đăng nhập thì chuyển sang /login, đăng nhập xong quay lại đúng trang này. */
export default function RequireAuth({ children }) {
    const { token } = useAuth()
    const location = useLocation()
    if (!token) return <Navigate to="/login" replace state={{ from: location.pathname }} />
    return children
}
