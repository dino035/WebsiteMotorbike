import { useEffect, useState } from 'react'
import client from '../api/client.js'
import FindPartsByVehicle from '../components/FindPartsByVehicle';

export default function HomePage() {
    const [apiStatus, setApiStatus] = useState('Đang kiểm tra kết nối backend...')

    useEffect(() => {
        client.get('/products')
            .then(() => setApiStatus('Đã kết nối được backend.'))
            .catch(() => setApiStatus('Chưa kết nối được backend (backend chưa chạy hoặc API chưa có).'))
    }, [])

    return (
        <section>
            <h1>Cửa hàng xe máy cũ và phụ tùng</h1>
            <p>{apiStatus}</p>
            <FindPartsByVehicle />
        </section>
    )
}