import { useContext } from 'react'
import { CartContext } from '../context/cart-context.js'

export default function useCart() {
    const ctx = useContext(CartContext)
    if (!ctx) throw new Error('useCart phải dùng bên trong CartProvider')
    return ctx
}
