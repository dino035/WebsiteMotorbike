import { NavLink, Outlet } from 'react-router-dom'

export default function MainLayout() {
    return (
        <div className="layout">
            <header className="header">
                <NavLink to="/" className="brand">MotoParts</NavLink>
                <nav className="nav">
                    <NavLink to="/products">Sản phẩm</NavLink>
                    <NavLink to="/login">Đăng nhập</NavLink>
                </nav>
            </header>
            <main className="content">
                <Outlet />
            </main>
        </div>
    )
}