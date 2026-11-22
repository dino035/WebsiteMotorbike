import { useState } from 'react';
import { Link } from 'react-router-dom';
import './ProductCard.css';

const priceFormat = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });

export default function ProductCard({ product }) {
    const isSold = product.status === 'SOLD';
    // Ảnh có đường dẫn nhưng tải hỏng (file đã bị xóa trên Supabase) thì trình duyệt vẽ alt text chữ to,
    // nhìn như lỗi giao diện. Bắt sự kiện lỗi để quay về khung "Chưa có ảnh" cho gọn.
    const [imageFailed, setImageFailed] = useState(false);
    const showImage = product.thumbnailUrl && !imageFailed;

    return (
        <Link to={`/products/${product.productId}`} className="product-card">
            <div className="product-card__image">
                {showImage ? (
                    <img
                        src={product.thumbnailUrl}
                        alt={product.name}
                        loading="lazy"
                        onError={() => setImageFailed(true)}
                    />
                ) : (
                    <span className="product-card__no-image">Chưa có ảnh</span>
                )}
            </div>

            <div className="product-card__body">
                <span className="product-card__type">{product.productType}</span>
                <h3 className="product-card__name">{product.name}</h3>
                <p className="product-card__detail">{product.detail}</p>

                <div className="product-card__footer">
                    <strong className="product-card__price">{priceFormat.format(product.price)}</strong>
                    <span className={isSold ? 'badge badge--sold' : 'badge badge--available'}>
                        {product.statusLabel}
                    </span>
                </div>
            </div>
        </Link>
    );
}
