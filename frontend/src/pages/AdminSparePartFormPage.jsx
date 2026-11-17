import { useEffect, useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import client from '../api/client';
import { apiError } from '../utils/apiError';
import AdminNav from '../components/AdminNav';
import PhotoManager from '../components/PhotoManager';
import './AdminProducts.css';

const EMPTY = {
    name: '',
    price: '',
    status: 'SOLD',
    partType: '',
    partOrigin: 'GENUINE',
};

export default function AdminSparePartFormPage() {
    const { productId } = useParams();
    const navigate = useNavigate();
    const isEdit = productId !== undefined;

    const [form, setForm] = useState(EMPTY);
    const [photos, setPhotos] = useState([]);
    const [loading, setLoading] = useState(isEdit);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    useEffect(() => {
        if (!isEdit) return;

        let ignore = false;

        client
            .get(`/products/${productId}`)
            .then((res) => {
                if (ignore) return;
                const p = res.data;
                setForm({
                    name: p.name,
                    price: String(p.price),
                    status: p.status,
                    partType: p.sparePart ? p.sparePart.partType : '',
                    partOrigin: p.sparePart && p.sparePart.partOrigin ? p.sparePart.partOrigin : 'GENUINE',
                });
                setPhotos(p.photos);
            })
            .catch((err) => {
                if (!ignore) setError(apiError(err, 'Không tải được thông tin phụ tùng.'));
            })
            .finally(() => {
                if (!ignore) setLoading(false);
            });

        return () => {
            ignore = true;
        };
    }, [isEdit, productId]);

    function handleChange(e) {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        setSaving(true);
        setError('');
        setMessage('');

        const body = {
            name: form.name.trim(),
            price: form.price === '' ? null : Number(form.price),
            status: form.status,
            partType: form.partType.trim(),
            partOrigin: form.partOrigin,
        };

        const request = isEdit
            ? client.put(`/admin/spare-parts/${productId}`, body)
            : client.post('/admin/spare-parts', body);

        request
            .then((res) => {
                if (isEdit) {
                    setMessage('Đã lưu thay đổi.');
                } else {
                    navigate(`/admin/products/spare-part/${res.data.productId}`, { replace: true });
                }
            })
            .catch((err) => setError(apiError(err, 'Không lưu được phụ tùng.')))
            .finally(() => setSaving(false));
    }

    if (loading) {
        return (
            <div className="ap-page">
                <AdminNav />
                <p className="ap-muted">Đang tải…</p>
            </div>
        );
    }

    return (
        <div className="ap-page">
            <AdminNav />

            <div className="ap-header">
                <h1>{isEdit ? 'Sửa phụ tùng' : 'Thêm phụ tùng'}</h1>
                <Link className="ap-btn" to="/admin/products">
                    ← Về danh sách
                </Link>
            </div>

            {message !== '' && <p className="ap-success">{message}</p>}
            {error !== '' && <p className="ap-error">{error}</p>}

            <form className="ap-form" onSubmit={handleSubmit}>
                <div className="ap-field">
                    <label htmlFor="sp-name">Tên phụ tùng *</label>
                    <input
                        id="sp-name"
                        name="name"
                        value={form.name}
                        onChange={handleChange}
                        required
                        placeholder="Ắc quy GS 12V"
                    />
                </div>

                <div className="ap-field">
                    <label htmlFor="sp-price">Giá (đồng) *</label>
                    <input
                        id="sp-price"
                        name="price"
                        type="number"
                        min="1"
                        step="1"
                        value={form.price}
                        onChange={handleChange}
                        required
                    />
                </div>

                <div className="ap-field">
                    <label htmlFor="sp-status">Tình trạng *</label>
                    <select id="sp-status" name="status" value={form.status} onChange={handleChange}>
                        <option value="AVAILABLE">Có sẵn</option>
                        <option value="SOLD">Hết hàng</option>
                    </select>

                    {!isEdit && (
                        <p className="ap-hint">
                            Phụ tùng mới phải lưu ở tình trạng Hết hàng. Thêm ảnh rồi mới
                            chuyển sang Có sẵn được.
                        </p>
                    )}
                </div>

                <div className="ap-field">
                    <label htmlFor="sp-partType">Loại phụ tùng *</label>
                    <input
                        id="sp-partType"
                        name="partType"
                        value={form.partType}
                        onChange={handleChange}
                        required
                        placeholder="Ắc quy"
                    />
                </div>

                <div className="ap-field">
                    <label htmlFor="sp-origin">Nguồn gốc *</label>
                    <select id="sp-origin" name="partOrigin" value={form.partOrigin} onChange={handleChange}>
                        <option value="GENUINE">Chính hãng</option>
                        <option value="AFTERMARKET">Phụ tùng bên thứ 3</option>
                    </select>
                </div>

                <button type="submit" className="ap-btn ap-btn-primary" disabled={saving}>
                    {saving ? 'Đang lưu…' : isEdit ? 'Lưu thay đổi' : 'Tạo phụ tùng'}
                </button>
            </form>

            {isEdit && (
                <PhotoManager
                    basePath={`/admin/spare-parts/${productId}`}
                    photos={photos}
                    onChange={setPhotos}
                />
            )}
        </div>
    );
}
