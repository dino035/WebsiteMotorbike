import { useEffect, useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import client from '../api/client';
import { apiError } from '../utils/apiError';
import AdminNav from '../components/AdminNav';
import PhotoManager from '../components/PhotoManager';
import ConditionForm from '../components/ConditionForm';
import './AdminProducts.css';

const EMPTY = {
    name: '',
    price: '',
    status: 'AVAILABLE',
    brand: '',
    model: '',
    registrationYear: '',
    ownerCount: '',
    accidentHistory: '',
    chassisNumber: '',
    engineNumber: '',
    odometerAtIntake: '',
};

export default function AdminMotorbikeFormPage() {
    const { productId } = useParams();
    const navigate = useNavigate();
    const isEdit = productId !== undefined;

    const [form, setForm] = useState(EMPTY);
    const [photos, setPhotos] = useState([]);
    const [condition, setCondition] = useState(null);
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
                const m = p.motorbike;
                setForm({
                    name: p.name,
                    price: String(p.price),
                    status: p.status,
                    brand: m.brand,
                    model: m.model,
                    registrationYear: String(m.registrationYear),
                    ownerCount: String(m.ownerCount),
                    accidentHistory: m.accidentHistory === null ? '' : m.accidentHistory,
                    chassisNumber: '',
                    engineNumber: '',
                    odometerAtIntake: String(m.odometerAtIntake),
                });
                setPhotos(p.photos);
                setCondition(m.condition);
            })
            .catch((err) => {
                if (!ignore) setError(apiError(err, 'Không tải được thông tin xe.'));
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
            brand: form.brand.trim(),
            model: form.model.trim(),
            registrationYear: form.registrationYear === '' ? null : Number(form.registrationYear),
            ownerCount: form.ownerCount === '' ? null : Number(form.ownerCount),
            accidentHistory: form.accidentHistory.trim() === '' ? null : form.accidentHistory.trim(),
            chassisNumber: form.chassisNumber.trim(),
            engineNumber: form.engineNumber.trim(),
            odometerAtIntake: form.odometerAtIntake === '' ? null : Number(form.odometerAtIntake),
        };

        const request = isEdit
            ? client.put(`/admin/motorbikes/${productId}`, body)
            : client.post('/admin/motorbikes', body);

        request
            .then((res) => {
                if (isEdit) {
                    setMessage('Đã lưu thay đổi.');
                } else {
                    navigate(`/admin/products/motorbike/${res.data.productId}`, { replace: true });
                }
            })
            .catch((err) => setError(apiError(err, 'Không lưu được xe.')))
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
                <h1>{isEdit ? 'Sửa xe' : 'Thêm xe'}</h1>
                <Link className="ap-btn" to="/admin/products">
                    ← Về danh sách
                </Link>
            </div>

            {message !== '' && <p className="ap-success">{message}</p>}
            {error !== '' && <p className="ap-error">{error}</p>}

            <form className="ap-form" onSubmit={handleSubmit}>
                <div className="ap-field">
                    <label htmlFor="mb-name">Tên xe *</label>
                    <input id="mb-name" name="name" value={form.name} onChange={handleChange}
                           required placeholder="Honda Wave Alpha 2019" />
                </div>

                <div className="ap-row">
                    <div className="ap-field">
                        <label htmlFor="mb-brand">Hãng *</label>
                        <input id="mb-brand" name="brand" value={form.brand} onChange={handleChange}
                               required placeholder="Honda" />
                    </div>
                    <div className="ap-field">
                        <label htmlFor="mb-model">Dòng xe *</label>
                        <input id="mb-model" name="model" value={form.model} onChange={handleChange}
                               required placeholder="Wave Alpha" />
                    </div>
                </div>

                <div className="ap-row">
                    <div className="ap-field">
                        <label htmlFor="mb-price">Giá (đồng) *</label>
                        <input id="mb-price" name="price" type="number" min="1" step="1"
                               value={form.price} onChange={handleChange} required />
                    </div>
                    <div className="ap-field">
                        <label htmlFor="mb-status">Tình trạng *</label>
                        <select id="mb-status" name="status" value={form.status} onChange={handleChange}>
                            <option value="AVAILABLE">Có sẵn</option>
                            <option value="SOLD">Hết hàng</option>
                        </select>
                    </div>
                </div>

                <div className="ap-row">
                    <div className="ap-field">
                        <label htmlFor="mb-year">Năm đăng ký *</label>
                        <input id="mb-year" name="registrationYear" type="number" min="1980" max="2100"
                               value={form.registrationYear} onChange={handleChange} required />
                    </div>
                    <div className="ap-field">
                        <label htmlFor="mb-owner">Số đời chủ *</label>
                        <input id="mb-owner" name="ownerCount" type="number" min="0"
                               value={form.ownerCount} onChange={handleChange} required />
                    </div>
                    <div className="ap-field">
                        <label htmlFor="mb-odo">Số km đã đi *</label>
                        <input id="mb-odo" name="odometerAtIntake" type="number" min="0"
                               value={form.odometerAtIntake} onChange={handleChange} required />
                    </div>
                </div>

                <div className="ap-row">
                    <div className="ap-field">
                        <label htmlFor="mb-chassis">Số khung *</label>
                        <input id="mb-chassis" name="chassisNumber" value={form.chassisNumber}
                               onChange={handleChange} required placeholder="RLHJC4710..." />
                    </div>
                    <div className="ap-field">
                        <label htmlFor="mb-engine">Số máy *</label>
                        <input id="mb-engine" name="engineNumber" value={form.engineNumber}
                               onChange={handleChange} required placeholder="JC47E..." />
                    </div>
                </div>

                {isEdit && (
                    <p className="ap-hint">
                        Vì bảo mật, API không trả về số khung và số máy. Hãy nhập lại đúng hai số
                        trên giấy đăng ký trước khi lưu.
                    </p>
                )}

                <div className="ap-field">
                    <label htmlFor="mb-accident">Lịch sử tai nạn</label>
                    <textarea id="mb-accident" name="accidentHistory" rows={3}
                              value={form.accidentHistory} onChange={handleChange}
                              placeholder="Để trống nếu xe chưa từng tai nạn" />
                </div>

                <button type="submit" className="ap-btn ap-btn-primary" disabled={saving}>
                    {saving ? 'Đang lưu…' : isEdit ? 'Lưu thay đổi' : 'Tạo xe'}
                </button>
            </form>

            {isEdit && (
                <>
                    <ConditionForm
                        productId={productId}
                        condition={condition}
                        onSaved={setCondition}
                    />
                    <PhotoManager
                        basePath={`/admin/motorbikes/${productId}`}
                        photos={photos}
                        onChange={setPhotos}
                    />
                </>
            )}
        </div>
    );
}
