import { useContext } from 'react'
import { NotificationContext } from '../context/notification-context.js'

export default function useNotifications() {
    const ctx = useContext(NotificationContext)
    if (!ctx) throw new Error('useNotifications phải dùng bên trong NotificationProvider')
    return ctx
}
