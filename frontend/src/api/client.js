import axios from 'axios'

export const TOKEN_KEY = 'motoparts.token'

/*
 * Gói miễn phí của Render cho máy chủ ngủ sau 15 phút không ai gọi. Lần gọi kế tiếp phải chờ
 * máy chủ khởi động lại: dựng container, chạy Spring Boot, kết nối Supabase — mất khoảng 100 giây.
 *
 * Để timeout 15 giây như trước thì lần đầu vào trang luôn báo "Không kết nối được máy chủ"
 * dù backend vẫn tốt, chỉ là đang dậy.
 *
 * Nhưng để 120 giây cho mọi lời gọi cũng sai: lúc mạng hỏng thật, người dùng ngồi chờ hai phút
 * mới biết. Nên chia làm hai mức.
 */
const WAKE_TIMEOUT = 120000   // lần đầu trong phiên: máy chủ có thể đang ngủ
const NORMAL_TIMEOUT = 20000  // đã có phản hồi: máy chủ tỉnh, chờ lâu nghĩa là hỏng thật

let serverAwake = false

/** Báo cho giao diện biết đang chờ máy chủ thức dậy, để hiện dải thông báo thay vì im lặng. */
function announce(waking) {
    try {
        window.dispatchEvent(new CustomEvent('motoparts:waking', { detail: waking }))
    } catch {
        // môi trường không có window (kiểm thử): bỏ qua
    }
}

const client = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    timeout: WAKE_TIMEOUT,
})

// Gắn token đăng nhập vào mọi yêu cầu (nếu có). Các API công khai (xem sản phẩm) bỏ qua token.
client.interceptors.request.use((config) => {
    try {
        const token = localStorage.getItem(TOKEN_KEY)
        if (token) config.headers.Authorization = `Bearer ${token}`
    } catch {
        // trình duyệt chặn localStorage: coi như chưa đăng nhập
    }

    config.timeout = serverAwake ? NORMAL_TIMEOUT : WAKE_TIMEOUT
    if (!serverAwake) announce(true)

    return config
})

client.interceptors.response.use(
    (response) => {
        // Có phản hồi nghĩa là máy chủ đã tỉnh, từ đây dùng thời gian chờ ngắn
        if (!serverAwake) {
            serverAwake = true
            announce(false)
        }
        return response
    },
    (error) => {
        // Máy chủ trả lời được, kể cả trả lỗi 4xx hay 5xx, vẫn nghĩa là nó đang chạy
        if (error.response && !serverAwake) {
            serverAwake = true
            announce(false)
        }
        if (!error.response) announce(false)
        return Promise.reject(error)
    },
)

export default client
