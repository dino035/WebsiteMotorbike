import axios from 'axios'

export const TOKEN_KEY = 'motoparts.token'

const client = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    timeout: 15000,
})

// Gắn token đăng nhập vào mọi yêu cầu (nếu có). Các API công khai (xem sản phẩm) bỏ qua token.
client.interceptors.request.use((config) => {
    try {
        const token = localStorage.getItem(TOKEN_KEY)
        if (token) config.headers.Authorization = `Bearer ${token}`
    } catch {
        // trình duyệt chặn localStorage: coi như chưa đăng nhập
    }
    return config
})

export default client
