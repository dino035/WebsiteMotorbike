import { useContext } from 'react'
import { AuthContext } from '../context/auth-context.js'

export default function useAuth() {
    const ctx = useContext(AuthContext)
    if (!ctx) throw new Error('useAuth phải dùng bên trong AuthProvider')
    return ctx
}
