import { useEffect, useState } from 'react'
import './WakeNotice.css'

/**
 * Dải thông báo hiện khi đang chờ máy chủ thức dậy.
 *
 * Máy chủ trên gói miễn phí của Render ngủ sau 15 phút không ai gọi. Lần gọi kế tiếp phải dựng
 * lại container rồi khởi động Spring Boot; log Render đo được "Started MotoPartsApplication in
 * 167.401 seconds". Không có dải này thì người dùng chỉ thấy chữ "Đang tải..." đứng yên rất lâu
 * và tưởng trang bị hỏng.
 *
 * Nghe sự kiện 'motoparts:waking' do src/api/client.js phát ra.
 */
export default function WakeNotice() {
    const [waking, setWaking] = useState(false)
    const [seconds, setSeconds] = useState(0)

    useEffect(() => {
        function onWaking(e) {
            setWaking(e.detail === true)
            if (e.detail !== true) setSeconds(0)
        }

        window.addEventListener('motoparts:waking', onWaking)
        return () => window.removeEventListener('motoparts:waking', onWaking)
    }, [])

    // Chỉ hiện sau 3 giây: lời gọi nhanh thì không cần làm phiền người dùng
    useEffect(() => {
        if (!waking) return undefined

        const timer = setInterval(() => setSeconds((s) => s + 1), 1000)
        return () => clearInterval(timer)
    }, [waking])

    if (!waking || seconds < 3) return null

    return (
        <div className="wake-notice" role="status">
            <span className="wake-notice__dot" />
            Máy chủ đang khởi động, lần truy cập đầu tiên có thể mất vài phút. Đã chờ {seconds} giây.
        </div>
    )
}
