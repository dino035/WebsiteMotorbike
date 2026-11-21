import { useEffect, useState } from 'react';
import client from '../api/client';
import { apiError } from '../utils/apiError';
import './ComparePage.css';

const MIN_COMPARE = 2;
const MAX_COMPARE = 4;

export default function ComparePage() {
    const [bikes, setBikes] = useState(null);     // danh sách xe để chọn
    const [picked, setPicked] = useState([]);     // mã các xe đang tick, theo thứ tự tick
    const [result, setResult] = useState(null);   // kết quả so sánh
    const [loading, setLoading] = useState(true);
    const [comparing, setComparing] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        let ignore = false;

        client
            .get('/products', { params: { type: 'motorbike', status: 'AVAILABLE', size: 50 } })
            .then((res) => {
                if (!ignore) setBikes(res.data.items);
            })
            .catch((err) => {
                if (!ignore) setError(apiError(err, 'Không tải được danh sách xe.'));
            })
            .finally(() => {
                if (!ignore) setLoading(false);
            });

        return () => {
            ignore = true;
        };
    }, []);

    function togglePick(productId) {
        setError('');
        setPicked((prev) => {
            if (prev.includes(productId)) {
                return prev.filter((id) => id !== productId);
            }
            if (prev.length >= MAX_COMPARE) {
                setError(`Chỉ so sánh được tối đa ${MAX_COMPARE} xe. Hãy bỏ bớt một xe trước.`);
                return prev;
            }
            return [...prev, productId];
        });
    }

    function handleCompare() {
        setComparing(true);
        setError('');

        client
            .get('/products/compare', { params: { ids: picked.join(',') } })
            .then((res) => setResult(res.data))
            .catch((err) => {
                setResult(null);
                setError(apiError(err, 'Không so sánh được, vui lòng thử lại.'));
            })
            .finally(() => setComparing(false));
    }

    function handleClear() {
        setPicked([]);
        setResult(null);
        setError('');
    }

    const items = result === null ? [] : result.items;

    return (
        <section className="cmp-page">
            <h1>So sánh xe</h1>
            <p className="cmp-intro">
                Chọn từ {MIN_COMPARE} đến {MAX_COMPARE} xe để xem bảng so sánh. Giá trị tốt nhất
                của từng tiêu chí sẽ được tô nổi.
            </p>

            {error !== '' && <p className="cmp-error">{error}</p>}

            {loading && <p className="cmp-muted">Đang tải danh sách xe…</p>}

            {bikes !== null && bikes.length === 0 && (
                <p className="cmp-muted">Hiện chưa có xe nào đang bán để so sánh.</p>
            )}

            {bikes !== null && bikes.length > 0 && (
                <>
                    <ul className="cmp-picker">
                        {bikes.map((bike) => {
                            const on = picked.includes(bike.productId);
                            return (
                                <li key={bike.productId}>
                                    <label className={on ? 'cmp-card cmp-card--on' : 'cmp-card'}>
                                        <input
                                            type="checkbox"
                                            className="cmp-card-check"
                                            checked={on}
                                            onChange={() => togglePick(bike.productId)}
                                        />
                                        {bike.thumbnailUrl === null ? (
                                            <span className="cmp-card-noimg">Chưa có ảnh</span>
                                        ) : (
                                            <img
                                                className="cmp-card-img"
                                                src={bike.thumbnailUrl}
                                                alt={bike.name}
                                            />
                                        )}
                                        <span className="cmp-card-name">{bike.name}</span>
                                        <span className="cmp-card-detail">{bike.detail}</span>
                                        <span className="cmp-card-price">
                                            {bike.price.toLocaleString('vi-VN')} đ
                                        </span>
                                    </label>
                                </li>
                            );
                        })}
                    </ul>

                    <div className="cmp-actions">
                        <button
                            type="button"
                            className="cmp-btn cmp-btn-primary"
                            disabled={picked.length < MIN_COMPARE || comparing}
                            onClick={handleCompare}
                        >
                            {comparing ? 'Đang so sánh…' : `So sánh ${picked.length} xe`}
                        </button>
                        <button type="button" className="cmp-btn" onClick={handleClear}>
                            Bỏ chọn hết
                        </button>
                    </div>
                </>
            )}

            {items.length > 0 && (
                <div className="cmp-table-wrap">
                    <table className="cmp-table">
                        <thead>
                        <tr>
                            <th>Tiêu chí</th>
                            {items.map((item) => (
                                <th key={item.productId}>{item.name}</th>
                            ))}
                        </tr>
                        </thead>
                        <tbody>
                        <tr>
                            <th>Ảnh</th>
                            {items.map((item) => (
                                <td key={item.productId}>
                                    {item.photoUrl === null ? (
                                        <span className="cmp-muted">Chưa có ảnh</span>
                                    ) : (
                                        <img className="cmp-photo" src={item.photoUrl} alt={item.name} />
                                    )}
                                </td>
                            ))}
                        </tr>

                        <tr>
                            <th>Giá</th>
                            {items.map((item) => (
                                <td key={item.productId} className={item.bestPrice ? 'cmp-best' : ''}>
                                    {item.price.toLocaleString('vi-VN')} đ
                                </td>
                            ))}
                        </tr>

                        <tr>
                            <th>Hãng / Dòng xe</th>
                            {items.map((item) => (
                                <td key={item.productId}>
                                    {item.brand} - {item.model}
                                </td>
                            ))}
                        </tr>

                        <tr>
                            <th>Năm đăng ký</th>
                            {items.map((item) => (
                                <td key={item.productId} className={item.bestYear ? 'cmp-best' : ''}>
                                    {item.registrationYear}
                                </td>
                            ))}
                        </tr>

                        <tr>
                            <th>Số đời chủ</th>
                            {items.map((item) => (
                                <td key={item.productId} className={item.bestOwners ? 'cmp-best' : ''}>
                                    {item.ownerCount}
                                </td>
                            ))}
                        </tr>

                        <tr>
                            <th>Số km đã đi</th>
                            {items.map((item) => (
                                <td key={item.productId} className={item.bestOdometer ? 'cmp-best' : ''}>
                                    {item.odometerAtIntake.toLocaleString('vi-VN')} km
                                </td>
                            ))}
                        </tr>

                        <tr>
                            <th>Điểm tình trạng</th>
                            {items.map((item) => (
                                <td key={item.productId} className={item.bestScore ? 'cmp-best' : ''}>
                                    {item.conditionScore === null ? (
                                        <span className="cmp-muted">Chưa chấm điểm</span>
                                    ) : (
                                        <>
                                            <strong>{item.conditionScore}</strong> / 10
                                            <div className="cmp-verdict">{item.verdict}</div>
                                        </>
                                    )}
                                </td>
                            ))}
                        </tr>

                        <tr>
                            <th>Lịch sử tai nạn</th>
                            {items.map((item) => (
                                <td key={item.productId}>{item.accidentHistory}</td>
                            ))}
                        </tr>
                        </tbody>
                    </table>

                    <p className="cmp-note">
                        Ô tô nền xanh là giá trị tốt nhất ở tiêu chí đó. Tiêu chí nào các xe bằng
                        nhau thì không tô.
                    </p>
                </div>
            )}
        </section>
    );
}
