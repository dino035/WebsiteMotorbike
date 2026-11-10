import { useEffect, useState } from 'react';
import client from '../api/client';
import ProductCard from '../components/ProductCard';
import './ProductListPage.css';

const PAGE_SIZE = 12;
const EMPTY_FILTERS = { keyword: '', type: '', status: '', minPrice: '', maxPrice: '' };

export default function ProductListPage() {
    const [form, setForm] = useState(EMPTY_FILTERS);       // những gì đang gõ trong ô lọc
    const [filters, setFilters] = useState(EMPTY_FILTERS); // bộ lọc đã bấm "Lọc"
    const [page, setPage] = useState(0);
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        let ignore = false;

        const params = { page, size: PAGE_SIZE };
        Object.entries(filters).forEach(([key, value]) => {
            if (value !== '') params[key] = value;
        });

        client
            .get('/products', { params })
            .then((res) => {
                if (!ignore) setData(res.data);
            })
            .catch(() => {
                if (!ignore) setError('Không tải được danh sách sản phẩm. Kiểm tra backend đã chạy chưa.');
            })
            .finally(() => {
                if (!ignore) setLoading(false);
            });

        return () => {
            ignore = true;
        };
    }, [filters, page]);

    function startLoading() {
        setLoading(true);
        setError('');
    }

    function handleChange(e) {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        startLoading();
        setFilters(form);
        setPage(0);
    }

    function handleReset() {
        startLoading();
        setForm(EMPTY_FILTERS);
        setFilters(EMPTY_FILTERS);
        setPage(0);
    }

    function goToPage(newPage) {
        startLoading();
        setPage(newPage);
    }

    return (
        <section className="product-list">
            <h1>Sản phẩm</h1>

            <form className="filter-bar" onSubmit={handleSubmit}>
                <input name="keyword" value={form.keyword} onChange={handleChange} placeholder="Tìm theo tên..." />
                <select name="type" value={form.type} onChange={handleChange}>
                    <option value="">Tất cả loại</option>
                    <option value="motorbike">Xe máy</option>
                    <option value="sparepart">Phụ tùng</option>
                </select>
                <select name="status" value={form.status} onChange={handleChange}>
                    <option value="">Mọi trạng thái</option>
                    <option value="AVAILABLE">Có sẵn</option>
                    <option value="SOLD">Hết hàng</option>
                </select>
                <input name="minPrice" type="number" min="0" value={form.minPrice} onChange={handleChange} placeholder="Giá từ" />
                <input name="maxPrice" type="number" min="0" value={form.maxPrice} onChange={handleChange} placeholder="Giá đến" />
                <button type="submit">Lọc</button>
                <button type="button" className="filter-bar__reset" onClick={handleReset}>Xóa lọc</button>
            </form>

            {loading && <p className="product-list__message">Đang tải...</p>}
            {error && <p className="product-list__message product-list__message--error">{error}</p>}

            {!loading && !error && data && (
                <>
                    <p className="product-list__count">Tìm thấy {data.totalItems} sản phẩm</p>

                    {data.items.length === 0 ? (
                        <p className="product-list__message">Không có sản phẩm nào khớp bộ lọc.</p>
                    ) : (
                        <div className="product-grid">
                            {data.items.map((p) => (
                                <ProductCard key={p.productId} product={p} />
                            ))}
                        </div>
                    )}

                    {data.totalPages > 1 && (
                        <div className="pagination">
                            <button type="button" disabled={page === 0} onClick={() => goToPage(page - 1)}>
                                Trước
                            </button>
                            <span>Trang {page + 1} / {data.totalPages}</span>
                            <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => goToPage(page + 1)}>
                                Sau
                            </button>
                        </div>
                    )}
                </>
            )}
        </section>
    );
}