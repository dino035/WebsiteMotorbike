import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import client from '../api/client';
import './ProductDetailPage.css';

const priceFormat = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });
const numberFormat = new Intl.NumberFormat('vi-VN');

export default function ProductDetailPage() {
    const { id } = useParams();
    const [result, setResult] = useState({ id: null, product: null, error: '' });

    useEffect(() => {
        let ignore = false;

        client
            .get(`/products/${encodeURIComponent(id)}`)
            .then((res) => {
                if (!ignore) setResult({ id, product: res.data, error: '' });
            })
            .catch((err) => {
                if (ignore) return;
                const message = err.response?.status === 404
                    ? 'Không tìm thấy sản phẩm này.'
                    : 'Không tải được sản phẩm. Kiểm tra backend đã chạy chưa.';
                setResult({ id, product: null, error: message });
            });

        return () => {
            ignore = true;
        };
    }, [id]);

    const loading = result.id !== id;
    const product = result.product;

    return (
        <section className="product-detail">
            <Link to="/products" className="product-detail__back">← Quay lại danh sách</Link>

            {loading && <p className="product-detail__message">Đang tải...</p>}
            {!loading && result.error && (
                <p className="product-detail__message product-detail__message--error">{result.error}</p>
            )}

            {!loading && product && (
                <div className="product-detail__layout">
                    <PhotoGallery key={product.productId} photos={product.photos} name={product.name} />

                    <div className="product-detail__info">
                        <span className="product-detail__type">{product.productType}</span>
                        <h1>{product.name}</h1>
                        <p className="product-detail__summary">{product.detail}</p>

                        <div className="product-detail__price-row">
                            <strong className="product-detail__price">{priceFormat.format(product.price)}</strong>
                            <span className={product.status === 'SOLD' ? 'badge badge--sold' : 'badge badge--available'}>
                {product.statusLabel}
              </span>
                        </div>

                        <h2>Thông số</h2>
                        <table className="spec-table">
                            <tbody>
                            {buildSpecs(product).map(([label, value]) => (
                                <tr key={label}>
                                    <th>{label}</th>
                                    <td>{value ?? '—'}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </section>
    );
}

function PhotoGallery({ photos, name }) {
    const [selected, setSelected] = useState(0);

    if (photos.length === 0) {
        return <div className="gallery__main gallery__main--empty">Chưa có ảnh</div>;
    }

    const current = photos[selected];

    return (
        <div className="gallery">
            <img className="gallery__main" src={current.imageUrl} alt={current.caption || name} />
            {photos.length > 1 && (
                <div className="gallery__thumbs">
                    {photos.map((photo, index) => (
                        <button
                            key={photo.photoId}
                            type="button"
                            className={index === selected ? 'gallery__thumb gallery__thumb--active' : 'gallery__thumb'}
                            onClick={() => setSelected(index)}
                        >
                            <img src={photo.imageUrl} alt={photo.caption || `${name} ${index + 1}`} />
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}

function buildSpecs(product) {
    if (product.motorbike) {
        const m = product.motorbike;
        return [
            ['Hãng', m.brand],
            ['Dòng xe', m.model],
            ['Năm đăng ký', m.registrationYear],
            ['Số đời chủ', m.ownerCount],
            ['Lịch sử va chạm', m.accidentHistory],
            ['Số km lúc nhập', m.odometerAtIntake == null ? null : `${numberFormat.format(m.odometerAtIntake)} km`],
        ];
    }
    if (product.sparePart) {
        return [
            ['Loại phụ tùng', product.sparePart.partType],
            ['Nguồn gốc', product.sparePart.partOriginLabel],
        ];
    }
    return [];
}