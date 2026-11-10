import { useCallback, useEffect, useMemo, useState } from 'react'
import client from '../api/client.js'
import useAuth from '../hooks/useAuth.js'
import { CartContext } from './cart-context.js'

/**
 * Giữ giỏ hàng hiện tại để thanh menu hiện số món và các trang dùng chung.
 * Mọi API giỏ hàng đều trả lại giỏ mới nhất nên mỗi thao tác chỉ cần lưu kết quả trả về.
 */
export default function CartProvider({ children }) {
    const { token, logout } = useAuth()
    const [stored, setStored] = useState({ token: null, cart: null })
    // Đăng xuất (hoặc đổi tài khoản) thì giỏ của người trước biến mất ngay, không cần xóa thủ công.
    const cart = stored.token === token ? stored.cart : null

    const save = useCallback((data) => setStored({ token, cart: data }), [token])

    // Token hết hạn hoặc tài khoản bị khóa: backend trả 401/403 -> đăng xuất để khách đăng nhập lại.
    const guard = useCallback((err) => {
        if (err.response?.status === 401) logout()
        throw err
    }, [logout])

    const refresh = useCallback(
        () => client.get('/cart').then((res) => { save(res.data); return res.data }).catch(guard),
        [save, guard],
    )
    const add = useCallback(
        (productId, quantity = 1) => client.post('/cart/items', { productId, quantity })
            .then((res) => { save(res.data); return res.data }).catch(guard),
        [save, guard],
    )
    const updateQuantity = useCallback(
        (cartItemId, quantity) => client.put(`/cart/items/${encodeURIComponent(cartItemId)}`, { quantity })
            .then((res) => { save(res.data); return res.data }).catch(guard),
        [save, guard],
    )
    const remove = useCallback(
        (cartItemId) => client.delete(`/cart/items/${encodeURIComponent(cartItemId)}`)
            .then((res) => { save(res.data); return res.data }).catch(guard),
        [save, guard],
    )

    // Có token thì tải giỏ một lần để menu hiện đúng số món.
    useEffect(() => {
        if (token) refresh().catch(() => {})
    }, [token, refresh])

    const value = useMemo(
        () => ({ cart, itemCount: cart?.itemCount ?? 0, refresh, add, updateQuantity, remove }),
        [cart, refresh, add, updateQuantity, remove],
    )
    return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
