import { NavLink, Outlet } from 'react-router-dom'
import useAuth from '../hooks/useAuth.js'
import useCart from '../hooks/useCart.js'

export default function MainLayout() {
    const { token, user, logout } = useAuth()
    const { itemCount } = useCart()

    return (
        <div className="layout">
            <header className="header">
                <NavLink to="/" className="brand">MotoParts</NavLink>
                <nav className="nav">
                    <NavLink to="/products">Sản phẩm</NavLink>
                    <NavLink to="/cart">Giỏ hàng{itemCount > 0 ? ` (${itemCount})` : ''}</NavLink>
                    {token ? (
                        <>
                            <span className="nav__user">{user?.email}</span>
                            <button type="button" className="nav__logout" onClick={logout}>Đăng xuất</button>
                        </>
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
        </div>
    )
}
