import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import client from '../api/client.js'
import ProductCard from '../components/ProductCard.jsx'
import FindPartsByVehicle from '../components/FindPartsByVehicle'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import './HomePage.css'

const FEATURED_SIZE = 8

/**
 * Trang chủ dành cho khách. Admin vào "/" thì chuyển thẳng sang khu quản trị: khối giới thiệu,
 * "Chọn xe của bạn" và lưới "Đang bán" đều là nội dung bán hàng, Admin không dùng tới.
 */
export default function HomePage() {
    const { user } = useAuth()
    const [result, setResult] = useState({ done: false, items: [], error: '' })

    useEffect(() => {
        let ignore = false
        client.get('/products', { params: { status: 'AVAILABLE', page: 0, size: FEATURED_SIZE } })
            .then((res) => { if (!ignore) setResult({ done: true, items: res.data.items ?? [], error: '' }) })
            .catch((err) => {
                if (!ignore) setResult({ done: true, items: [], error: apiError(err, 'Chưa tải được sản phẩm.') })
            })
        return () => { ignore = true }
    }, [])

    if (user?.role === 'ADMIN') return <Navigate to="/admin/orders" replace />

    return (
        <>
            <section className="hero">
                <div className="hero__text">
                    <p className="hero__eyebrow">Xe máy cũ · Phụ tùng chính hãng</p>
                    <h1 className="hero__title">Xe đã kiểm tra tình trạng, phụ tùng lắp vừa xe bạn</h1>
                    <p className="hero__lead">
                        Mỗi chiếc xe đều được chấm điểm tình trạng theo năm hạng mục trước khi lên sàn.
                        Phụ tùng tra theo đúng dòng xe và năm sản xuất, không lo mua nhầm.
                    </p>
                    <div className="hero__actions">
                        <Link to="/products" className="btn">Xem sản phẩm</Link>
                        <Link to="/compare" className="btn btn--ghost">So sánh xe</Link>
                    </div>
                </div>

                <ul className="hero__points">
                    <li>
                        <strong>Chấm điểm tình trạng</strong>
                        <span>Năm hạng mục, hệ số tự tính, công khai trên trang chi tiết xe</span>
                    </li>
                    <li>
                        <strong>Tra phụ tùng theo xe</strong>
                        <span>Chọn hãng, dòng và năm để lọc đúng phụ tùng lắp vừa</span>
                    </li>
                    <li>
                        <strong>Lắp đặt tại cửa hàng</strong>
                        <span>Đặt lịch khi mua phụ tùng, phí công 50.000đ</span>
                    </li>
                </ul>
            </section>

            <FindPartsByVehicle />

            <section className="home-section">
                <div className="home-section__head">
                    <h2>Đang bán</h2>
                    <Link to="/products">Xem tất cả →</Link>
                </div>

                {!result.done && <p className="home-section__message">Đang tải...</p>}
                {result.done && result.error && (
                    <p className="home-section__message home-section__message--error">{result.error}</p>
                )}
                {result.done && !result.error && result.items.length === 0 && (
                    <p className="home-section__message">Chưa có sản phẩm nào đang bán.</p>
                )}
                {result.items.length > 0 && (
                    <div className="product-grid">
                        {result.items.map((p) => <ProductCard key={p.productId} product={p} />)}
                    </div>
                )}
            </section>
        </>
    )
}
