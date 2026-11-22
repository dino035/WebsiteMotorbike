import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client from '../api/client';
import useAuth from '../hooks/useAuth.js';
import { apiError } from '../utils/apiError.js';
import { formatDateTime } from '../utils/format.js';
import './ProductReviews.css';

export default function ProductReviews({ productId }) {
    const { token } = useAuth();

    // Cùng kiểu state với ProductDetailPage: giữ luôn id đã tải để biết khi nào đang tải lại.
    const [result, setResult] = useState({ id: null, data: null, error: '' });
    const [reloadKey, setReloadKey] = useState(0);

    const [rating, setRating] = useState(5);
    const [comment, setComment] = useState('');
    const [sending, setSending] = useState(false);
    const [formError, setFormError] = useState('');
    const [done, setDone] = useState(false);

    useEffect(() => {
        let ignore = false;

        client
            .get(`/products/${encodeURIComponent(productId)}/reviews`)
            .then((res) => {
                if (!ignore) setResult({ id: productId, data: res.data, error: '' });
            })
            .catch((err) => {
                if (!ignore) {
                    setResult({ id: productId, data: null, error: apiError(err, 'Không tải được đánh giá.') });
                }
            });

        return () => {
            ignore = true;
        };
    }, [productId, reloadKey]);

    function handleSubmit(event) {
        event.preventDefault();
        setFormError('');
        setSending(true);

        client
            .post(`/products/${encodeURIComponent(productId)}/reviews`, {
                rating: Number(rating),
                comment,
            })
            .then(() => {
                setDone(true);
                setComment('');
                setReloadKey((key) => key + 1);   // tải lại danh sách từ server
            })
            .catch((err) => setFormError(apiError(err, 'Không gửi được đánh giá.')))
            .finally(() => setSending(false));
    }

    const loading = result.id !== productId;
    const summary = result.data;

    return (
        <section className="reviews">
            <h2>Đánh giá sản phẩm</h2>

            {loading && <p className="reviews__message">Đang tải đánh giá...</p>}
            {!loading && result.error && (
                <p className="reviews__message reviews__message--error">{result.error}</p>
            )}

            {!loading && summary && summary.count === 0 && (
                <p className="reviews__message">Chưa có đánh giá nào cho sản phẩm này.</p>
            )}

            {!loading && summary && summary.count > 0 && (
                <>
                    <p className="reviews__summary">
                        <Stars value={Math.round(summary.average)} />
                        <strong>{summary.average}</strong> / 5
                        <span className="reviews__count">({summary.count} đánh giá)</span>
                    </p>

                    <ul className="reviews__list">
                        {summary.items.map((item) => (
                            <li key={item.reviewId} className="reviews__item">
                                <div className="reviews__head">
                                    <Stars value={item.rating} />
                                    <span className="reviews__author">{item.reviewer}</span>
                                    <span className="reviews__date">{formatDateTime(item.createdAt)}</span>
                                </div>
                                <p className="reviews__comment">{item.comment}</p>
                            </li>
                        ))}
                    </ul>
                </>
            )}

            {done && <p className="reviews__thanks">Cảm ơn bạn đã đánh giá sản phẩm.</p>}

            {!done && !token && (
                <p className="reviews__message">
                    <Link to="/login">Đăng nhập</Link> để đánh giá sản phẩm bạn đã mua.
                </p>
            )}

            {!done && token && (
                <form className="reviews__form" onSubmit={handleSubmit}>
                    <h3>Viết đánh giá</h3>

                    <label>
                        Số sao
                        <select value={rating} onChange={(e) => setRating(e.target.value)}>
                            <option value={5}>5 — Rất hài lòng</option>
                            <option value={4}>4 — Hài lòng</option>
                            <option value={3}>3 — Bình thường</option>
                            <option value={2}>2 — Chưa hài lòng</option>
                            <option value={1}>1 — Rất tệ</option>
                        </select>
                    </label>

                    <label>
                        Nhận xét
                        <textarea
                            rows={4}
                            maxLength={1000}
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            placeholder="Sản phẩm dùng thế nào, giao hàng ra sao..."
                        />
                    </label>
                    <span className="reviews__hint">{comment.length}/1000 ký tự</span>

                    {formError && <p className="reviews__message reviews__message--error">{formError}</p>}

                    <button type="submit" disabled={sending}>
                        {sending ? 'Đang gửi...' : 'Gửi đánh giá'}
                    </button>
                </form>
            )}
        </section>
    );
}

function Stars({ value }) {
    return <span className="reviews__stars">{'★'.repeat(value)}{'☆'.repeat(5 - value)}</span>;
}