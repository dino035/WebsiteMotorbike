import { Link } from 'react-router-dom';

const priceFormat = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });

export default function ProductCard({ product }) {
    const isSold = product.status === 'SOLD';

    return (
        <Link to={`/products/${product.productId}`} className="product-card">
            <div className="product-card__image">
                {product.thumbnailUrl ? (
                    <img src={product.thumbnailUrl} alt={product.name} loading="lazy" />
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