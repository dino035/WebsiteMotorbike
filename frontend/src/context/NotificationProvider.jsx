import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import client from '../api/client.js'
import useAuth from '../hooks/useAuth.js'
import { NotificationContext } from './notification-context.js'

const POLL_MS = 60_000

/**
 * Giữ số thông báo chưa đọc cho thanh menu. Tải lại khi đổi trang (đặt hàng xong, gửi bảo hành xong... sẽ có
 * thông báo mới) và mỗi phút một lần để thông báo do Admin xử lý hiện ra mà khách không cần tải lại trang.
 */
export default function NotificationProvider({ children }) {
    const { token, logout } = useAuth()
    const { pathname } = useLocation()
    const [stored, setStored] = useState({ token: null, count: 0 })
    // Đăng xuất (hoặc đổi tài khoản) thì số của người trước biến mất ngay.
    const unreadCount = stored.token === token ? stored.count : 0

    const refresh = useCallback(
        () => client.get('/notifications/unread-count')
            .then((res) => setStored({ token, count: res.data.count }))
            .catch((err) => { if (err.response?.status === 401) logout() }),
        [token, logout],
    )

    useEffect(() => {
        if (!token) return undefined
        refresh()
        const timer = setInterval(refresh, POLL_MS)
        return () => clearInterval(timer)
    }, [token, refresh, pathname])

    const value = useMemo(() => ({ unreadCount, refresh }), [unreadCount, refresh])
    return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}
