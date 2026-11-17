import { useEffect, useState } from 'react'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import './AdminPages.css'
import './AdminCompatibilityPage.css'

/** Admin khai báo phụ tùng lắp vừa dòng xe nào, theo khoảng năm. */
export default function AdminCompatibilityPage() {
    const { logout } = useAuth()
    // Dữ liệu cho các ô chọn
    const [parts, setParts] = useState([]);
    const [brands, setBrands] = useState([]);
    const [models, setModels] = useState([]);
    const [years, setYears] = useState([]);

    // Giá trị đang chọn trong form
    const [sparePartId, setSparePartId] = useState('');
    const [brand, setBrand] = useState('');
    const [model, setModel] = useState('');
    const [yearFrom, setYearFrom] = useState('');
    const [yearTo, setYearTo] = useState('');

    // Kết quả
    const [rules, setRules] = useState([]);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);

    // 1. Tải danh sách phụ tùng và hãng xe, chỉ một lần
    useEffect(() => {
        let ignore = false;
        client
            .get('/products', { params: { type: 'sparepart', size: 50 } })
            .then((res) => { if (!ignore) setParts(res.data.items); })
            .catch(() => { if (!ignore) setError('Không tải được danh sách phụ tùng.'); });
        client
            .get('/vehicle-models/brands')
            .then((res) => { if (!ignore) setBrands(res.data); })
            .catch(() => { if (!ignore) setError('Không tải được danh sách hãng xe.'); });
        return () => { ignore = true; };
    }, []);

    // 2. Chọn hãng thì tải các dòng xe
    useEffect(() => {
        if (!brand) { setModels([]); return undefined; }
        let ignore = false;
        client
            .get('/vehicle-models/models', { params: { brand } })
            .then((res) => { if (!ignore) setModels(res.data); })
            .catch(() => { if (!ignore) setError('Không tải được danh sách dòng xe.'); });
        return () => { ignore = true; };
    }, [brand]);

    // 3. Chọn dòng xe thì tải các năm có trong dữ liệu
    useEffect(() => {
        if (!brand || !model) { setYears([]); return undefined; }
        let ignore = false;
        client
            .get('/vehicle-models/years', { params: { brand, model } })
            .then((res) => { if (!ignore) setYears(res.data); })
            .catch(() => { if (!ignore) setError('Không tải được danh sách năm.'); });
        return () => { ignore = true; };
    }, [brand, model]);

    // 4. Chọn phụ tùng thì tải các luật đã có của nó
    useEffect(() => {
        setMessage('');
        if (!sparePartId) { setRules([]); return undefined; }
        let ignore = false;
        client
            .get('/admin/compatibility-rules', { params: { sparePartId } })
            .then((res) => { if (!ignore) setRules(res.data); })
            .catch(() => { if (!ignore) setError('Không tải được danh sách luật.'); });
        return () => { ignore = true; };
    }, [sparePartId]);

    function handleBrandChange(e) {
        setBrand(e.target.value);
        setModel('');
        setYearFrom('');
        setYearTo('');
    }

    function handleModelChange(e) {
        setModel(e.target.value);
        setYearFrom('');
        setYearTo('');
    }

    function handleSubmit(e) {
        e.preventDefault();
        setSaving(true);
        setMessage('');
        setError('');
        client
            .post('/admin/compatibility-rules', {
                sparePartId,
                brand,
                model,
                yearFrom: Number(yearFrom),
                yearTo: Number(yearTo),
            })
            .then((res) => {
                setMessage(`Đã tạo ${res.data.created} luật, bỏ qua ${res.data.skipped} luật đã có.`);
                return client.get('/admin/compatibility-rules', { params: { sparePartId } });
            })
            .then((res) => setRules(res.data))
            .catch((err) => {
                if (err.response?.status === 401) logout()
                else setError(apiError(err, 'Không tạo được luật.'))
            })
            .finally(() => setSaving(false));
    }

    function handleDelete(ruleId) {
        if (!window.confirm('Xóa luật này?')) return;
        setError('');
        client
            .delete(`/admin/compatibility-rules/${ruleId}`)
            .then(() => {
                setRules((prev) => prev.filter((r) => r.ruleId !== ruleId));
                setMessage('Đã xóa luật.');
            })
            .catch((err) => setError(apiError(err, 'Không xóa được luật.')))
    }

    const canSubmit = sparePartId && brand && model && yearFrom && yearTo && !saving;

    return (
        <section className="admin">
            <h1>Quản trị</h1>
            <AdminNav />

            <h2>Luật tương thích phụ tùng</h2>
            <p className="admin__message">
                Khai báo một phụ tùng lắp vừa dòng xe nào, trong khoảng năm nào.
                Năm đã có luật sẽ được bỏ qua, không tạo trùng.
            </p>

            <form className="compat-form" onSubmit={handleSubmit}>
                <label>
                    Phụ tùng
                    <select value={sparePartId} onChange={(e) => setSparePartId(e.target.value)}>
                        <option value="">-- Chọn phụ tùng --</option>
                        {parts.map((p) => (
                            <option key={p.productId} value={p.productId}>{p.name}</option>
                        ))}
                    </select>
                </label>

                <label>
                    Hãng xe
                    <select value={brand} onChange={handleBrandChange}>
                        <option value="">-- Chọn hãng --</option>
                        {brands.map((b) => (
                            <option key={b} value={b}>{b}</option>
                        ))}
                    </select>
                </label>

                <label>
                    Dòng xe
                    <select value={model} onChange={handleModelChange} disabled={!brand}>
                        <option value="">-- Chọn dòng xe --</option>
                        {models.map((m) => (
                            <option key={m} value={m}>{m}</option>
                        ))}
                    </select>
                </label>

                <label>
                    Từ năm
                    <select value={yearFrom} onChange={(e) => setYearFrom(e.target.value)} disabled={!model}>
                        <option value="">--</option>
                        {years.map((y) => (
                            <option key={y.modelId} value={y.year}>{y.year}</option>
                        ))}
                    </select>
                </label>

                <label>
                    Đến năm
                    <select value={yearTo} onChange={(e) => setYearTo(e.target.value)} disabled={!model}>
                        <option value="">--</option>
                        {years.map((y) => (
                            <option key={y.modelId} value={y.year}>{y.year}</option>
                        ))}
                    </select>
                </label>

                <button type="submit" disabled={!canSubmit}>
                    {saving ? 'Đang lưu...' : 'Tạo luật'}
                </button>
            </form>

            {message && <p className="admin__message compat-ok">{message}</p>}
            {error && <p className="admin__message admin__message--error">{error}</p>}

            <h2>Luật đã khai báo</h2>
            {!sparePartId ? (
                <p className="admin__message">Chọn một phụ tùng để xem các luật của nó.</p>
            ) : rules.length === 0 ? (
                <p className="admin__message">Phụ tùng này chưa có luật nào.</p>
            ) : (
                <div className="admin__table-wrap">
                    <table className="admin__table">
                        <thead>
                        <tr>
                            <th>Hãng</th><th>Dòng xe</th><th>Năm</th><th></th>
                        </tr>
                        </thead>
                        <tbody>
                        {rules.map((r) => (
                            <tr key={r.ruleId}>
                                <td>{r.brand}</td>
                                <td>{r.model}</td>
                                <td>{r.year}</td>
                                <td>
                                    <button type="button" className="compat-delete" onClick={() => handleDelete(r.ruleId)}>
                                        Xóa
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    )
}