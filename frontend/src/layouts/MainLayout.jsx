import { NavLink, Outlet, useLocation } from 'react-router-dom'
import AccountMenu from '../components/AccountMenu.jsx'
import ChatWidget from '../components/ChatWidget.jsx'
import useAuth from '../hooks/useAuth.js'
import useCart from '../hooks/useCart.js'
import useNotifications from '../hooks/useNotifications.js'

/**
 * Khung chung của mọi trang. Thanh điều hướng đổi theo vai trò, giống bản JSP:
 * Admin chỉ thấy phần quản trị, không thấy giỏ hàng, đơn hàng, bảo hành, địa chỉ, xe của tôi hay so sánh xe —
 * đó là chức năng của khách hàng, Admin không dùng tới.
 *
 * Các mục cá nhân gom vào nút "Tài khoản" bung xuống, nên thanh điều hướng nhiều nhất chỉ 4 mục và
 * không bao giờ tràn xuống dòng thứ hai.
 */
export default function MainLayout() {
    const { token, user, logout } = useAuth()
    const { itemCount } = useCart()
    const { unreadCount } = useNotifications()
    const { pathname } = useLocation()

    const isAdmin = user?.role === 'ADMIN'
    // "Quản trị" phải sáng ở mọi trang /admin, không chỉ riêng /admin/orders
    const adminClass = pathname.startsWith('/admin') ? 'active' : undefined

    return (
        <div className="layout">
            <header className="header">
                <NavLink to="/" className="brand">MotoParts</NavLink>

                <nav className="nav">
                    <NavLink to="/products">Sản phẩm</NavLink>

                    {isAdmin ? (
                        <NavLink to="/admin/orders" className={adminClass}>Quản trị</NavLink>
                    ) : (
                        <NavLink to="/compare">So sánh xe</NavLink>
                    )}

                    {token && !isAdmin && (
                        <NavLink to="/cart">Giỏ hàng{itemCount > 0 ? ` (${itemCount})` : ''}</NavLink>
                    )}

                    {token ? (
                        <AccountMenu
                            email={user?.email}
                            unreadCount={unreadCount}
                            isAdmin={isAdmin}
                            onLogout={logout}
                        />
                    ) : (
                        <>
                            <NavLink to="/login">Đăng nhập</NavLink>
                            <NavLink to="/register">Đăng ký</NavLink>
                        </>
                    )}
                </nav>
            </header>

            <main className="content">
                <Outlet />
            </main>

            {/* Khung chat nổi: tự ẩn với admin và khách chưa đăng nhập */}
            <ChatWidget />
        </div>
    )
}