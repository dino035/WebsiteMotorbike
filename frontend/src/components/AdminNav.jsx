import { NavLink } from 'react-router-dom'

/** Thanh chuyển giữa các trang quản trị. */
export default function AdminNav() {
    return (
        <nav className="admin-nav" aria-label="Quản trị">
            <NavLink to="/admin/orders">Đơn hàng</NavLink>
            <NavLink to="/admin/installations">Lịch lắp đặt</NavLink>
            <NavLink to="/admin/warranty">Bảo hành</NavLink>
            <NavLink to="/admin/compatibility">Luật tương thích</NavLink>
            <NavLink to="/admin/products">Sản phẩm</NavLink>
            <NavLink to="/admin/chat">Câu hỏi chat</NavLink>
        </nav>
    )
}
