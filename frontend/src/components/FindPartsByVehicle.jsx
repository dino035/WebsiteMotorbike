import { useState } from 'react';
import VehicleModelPicker from './VehicleModelPicker';
import ProductCard from './ProductCard';
import client from '../api/client';
import './FindPartsByVehicle.css';

/** Khung "Chọn xe của bạn" ở trang chủ: chọn xe rồi xem phụ tùng lắp vừa. */
export default function FindPartsByVehicle() {
    const [vehicle, setVehicle] = useState(null);
    const [parts, setParts] = useState(null);      // null = chưa bấm tìm lần nào
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // Đổi xe thì xóa kết quả cũ, tránh hiện phụ tùng của xe trước
    function handleVehicleChange(next) {
        setVehicle(next);
        setParts(null);
        setError('');
    }

    function handleSearch() {
        if (!vehicle) return;
        setLoading(true);
        setError('');
        client
            .get(`/vehicle-models/${vehicle.modelId}/compatible-parts`)
            .then((res) => setParts(res.data))
            .catch(() => setError('Không tải được danh sách phụ tùng. Vui lòng thử lại.'))
            .finally(() => setLoading(false));
    }

    return (
        <section className="find-parts">
            <h2>Chọn xe của bạn</h2>
            <p className="find-parts__hint">
                Chọn đủ hãng, dòng xe và năm để xem những phụ tùng lắp vừa xe bạn.
            </p>

            <VehicleModelPicker onChange={handleVehicleChange} />

            <div className="find-parts__actions">
                <button
                    type="button"
                    className="find-parts__button"
                    onClick={handleSearch}
                    disabled={!vehicle || loading}
                >
                    {loading ? 'Đang tìm...' : 'Xem phụ tùng hợp với xe này'}
                </button>

                {vehicle && (
                    <span className="find-parts__selected">
                        Đã chọn: {vehicle.brand} {vehicle.model} {vehicle.year}
                    </span>
                )}
            </div>

            {error && <p className="find-parts__error">{error}</p>}

            {parts !== null && !loading && !error && (
                parts.length === 0 ? (
                    <p className="find-parts__empty">
                        Chưa có phụ tùng nào được khai báo cho xe này.
                    </p>
                ) : (
                    <>
                        <p className="find-parts__count">Tìm thấy {parts.length} phụ tùng.</p>
                        <div className="product-grid">
                            {parts.map((p) => (
                                <ProductCard key={p.productId} product={p} />
                            ))}
                        </div>
                    </>
                )
            )}
        </section>
    );
}