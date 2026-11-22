import { useEffect, useRef, useState } from 'react'
import { NavLink } from 'react-router-dom'
import './AccountMenu.css'

/**
 * Nhóm các mục cá nhân vào một nút "Tài khoản" bung xuống.
 * Lý do: để thẳng hàng thì thanh điều hướng của khách có tới 11 mục, màn hình hẹp một chút là tràn xuống
 * dòng thứ hai và nhìn như vỡ. Gom lại còn 4 mục trên thanh thì không bao giờ tràn nữa.
 */
export default function AccountMenu({ email, unreadCount, isAdmin, onLogout }) {
    const [open, setOpen] = useState(false)
    const boxRef = useRef(null)

    // Bấm ra ngoài hoặc nhấn Esc thì đóng
    useEffect(() => {
        if (!open) return undefined
        function onPointerDown(event) {
            if (boxRef.current && !boxRef.current.contains(event.target)) setOpen(false)
        }
        function onKey(event) {
            if (event.key === 'Escape') setOpen(false)
        }
        document.addEventListener('pointerdown', onPointerDown)
        document.addEventListener('keydown', onKey)
        return () => {
            document.removeEventListener('pointerdown', onPointerDown)
            document.removeEventListener('keydown', onKey)
        }
    }, [open])

    const close = () => setOpen(false)

    return (
        <div className="account" ref={boxRef}>
            <button
                type="button"
                className={open ? 'account__toggle account__toggle--open' : 'account__toggle'}
                aria-expanded={open}
                aria-haspopup="true"
                onClick={() => setOpen((v) => !v)}
            >
                Tài khoản
                {unreadCount > 0 && <span className="account__dot" aria-hidden="true" />}
                <span className="account__caret" aria-hidden="true">▾</span>
            </button>

            {open && (
                <div className="account__menu" role="menu">
                    <p className="account__email">{email}</p>

                    {!isAdmin && (
                        <>
                            <NavLink to="/vehicles" onClick={close}>Xe của tôi</NavLink>
                            <NavLink to="/orders" onClick={close}>Đơn hàng</NavLink>
                            <NavLink to="/warranty" onClick={close}>Bảo hành</NavLink>
                            <NavLink to="/addresses" onClick={close}>Địa chỉ</NavLink>
                        </>
                    )}

                    <NavLink to="/notifications" onClick={close}>
                        Thông báo{unreadCount > 0 ? ` (${unreadCount})` : ''}
                    </NavLink>

                    <button type="button" className="account__logout" onClick={() => { close(); onLogout() }}>
                        Đăng xuất
                    </button>
                </div>
            )}
        </div>
    )
}
