import { Navigate, useLocation } from 'react-router-dom'
import useAuth from '../hooks/useAuth.js'

/**
 * Trang quản trị: chưa đăng nhập thì chuyển sang /login; đã đăng nhập nhưng không phải ADMIN thì báo không có quyền.
 * Đây chỉ là che giao diện; quyền thật do backend kiểm tra (/api/admin/** trả 403 với người không phải ADMIN).
 */
export default function RequireAdmin({ children }) {
    const { token, user } = useAuth()
    const location = useLocation()
    if (!token) return <Navigate to="/login" replace state={{ from: location.pathname }} />
    if (user?.role !== 'ADMIN') {
        return <p style={{ padding: '24px 0', color: '#c62828' }}>Bạn không có quyền truy cập trang quản trị.</p>
    }
    return children
}
