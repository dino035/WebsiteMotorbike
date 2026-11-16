import { NavLink } from 'react-router-dom'

/** Thanh chuyển giữa các trang quản trị. */
export default function AdminNav() {
    return (
        <nav className="admin-nav" aria-label="Quản trị">
            <NavLink to="/admin/orders">Đơn hàng</NavLink>
            <NavLink to="/admin/installations">Lịch lắp đặt</NavLink>
            <NavLink to="/admin/warranty">Bảo hành</NavLink>
        </nav>
    )
}
