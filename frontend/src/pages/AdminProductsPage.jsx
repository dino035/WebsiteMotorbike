import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client from '../api/client';
import { apiError } from '../utils/apiError';
import AdminNav from '../components/AdminNav';
import './AdminProducts.css';

const PAGE_SIZE = 20;

export default function AdminProductsPage() {
    const [type, setType] = useState('');       // '' | 'motorbike' | 'sparepart'
    const [page, setPage] = useState(0);
    const [data, setData] = useState(null);     // null = đang tải lần đầu
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        let ignore = false;

        const params = { page, size: PAGE_SIZE };
        if (type !== '') params.type = type;

        client
            .get('/products', { params })
            .then((res) => {
                if (!ignore) {
                    setData(res.data);
                    setError('');
                }
            })
            .catch((err) => {
                if (!ignore) setError(apiError(err, 'Không tải được danh sách sản phẩm.'));
            })
            .finally(() => {
                if (!ignore) setLoading(false);
            });

        return () => {
            ignore = true;
        };
    }, [type, page, reloadKey]);

    function handleTypeChange(e) {
        setType(e.target.value);
        setPage(0);
        setMessage('');
    }

    function handleDelete(product) {
        const ok = window.confirm(`Xóa "${product.name}"? Thao tác này không hoàn lại được.`);
        if (!ok) return;

        const url =
            product.typeCode === 'Motorbike'
                ? `/admin/motorbikes/${product.productId}`
                : `/admin/spare-parts/${product.productId}`;

        setMessage('');
        setError('');

        client
            .delete(url)
            .then(() => {
                setMessage(`Đã xóa "${product.name}".`);
                setReloadKey((prev) => prev + 1);
            })
            .catch((err) => setError(apiError(err, 'Không xóa được sản phẩm.')));
    }

    const items = data ? data.items : [];
    const totalPages = data ? data.totalPages : 0;

    return (
        <div className="ap-page">
            <AdminNav />

            <div className="ap-header">
                <h1>Quản trị sản phẩm</h1>
                <div className="ap-actions">
                    <Link className="ap-btn ap-btn-primary" to="/admin/products/motorbike/new">
                        + Thêm xe
                    </Link>
                    <Link className="ap-btn ap-btn-primary" to="/admin/products/spare-part/new">
                        + Thêm phụ tùng
                    </Link>
                </div>
            </div>

            <div className="ap-filter">
                <label htmlFor="ap-type">Loại sản phẩm</label>
                <select id="ap-type" value={type} onChange={handleTypeChange}>
                    <option value="">Tất cả</option>
                    <option value="motorbike">Xe máy</option>
                    <option value="sparepart">Phụ tùng</option>
                </select>
            </div>

            {message !== '' && <p className="ap-success">{message}</p>}
            {error !== '' && <p className="ap-error">{error}</p>}

            {loading && data === null && <p className="ap-muted">Đang tải…</p>}

            {data !== null && items.length === 0 && (
                <p className="ap-muted">Chưa có sản phẩm nào.</p>
            )}

            {items.length > 0 && (
                <table className="ap-table">
                    <thead>
                        <tr>
                            <th>Ảnh</th>
                            <th>Tên</th>
                            <th>Loại</th>
                            <th>Chi tiết</th>
                            <th>Giá</th>
                            <th>Tình trạng</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        {items.map((p) => (
                            <tr key={p.productId}>
                                <td>
                                    {p.thumbnailUrl ? (
                                        <img className="ap-thumb" src={p.thumbnailUrl} alt={p.name} />
                                    ) : (
                                        <span className="ap-no-thumb">—</span>
                                    )}
                                </td>
                                <td>{p.name}</td>
                                <td>{p.productType}</td>
                                <td>{p.detail}</td>
                                <td>{p.price.toLocaleString('vi-VN')} đ</td>
                                <td>{p.statusLabel}</td>
                                <td className="ap-row-actions">
                                    <Link
                                        className="ap-btn"
                                        to={
                                            p.typeCode === 'Motorbike'
                                                ? `/admin/products/motorbike/${p.productId}`
                                                : `/admin/products/spare-part/${p.productId}`
                                        }
                                    >
                                        Sửa
                                    </Link>
                                    <button
                                        type="button"
                                        className="ap-btn ap-btn-danger"
                                        onClick={() => handleDelete(p)}
                                    >
                                        Xóa
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}

            {totalPages > 1 && (
                <div className="ap-paging">
                    <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>
                        Trang trước
                    </button>
                    <span>
                        Trang {page + 1} / {totalPages}
                    </span>
                    <button
                        type="button"
                        disabled={page + 1 >= totalPages}
                        onClick={() => setPage(page + 1)}
                    >
                        Trang sau
                    </button>
                </div>
            )}
        </div>
    );
}
