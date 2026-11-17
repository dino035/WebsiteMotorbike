import { useState } from 'react';
import client from '../api/client';
import { apiError } from '../utils/apiError';

const LEVELS = [
    { value: 'POOR', label: 'Kém' },
    { value: 'FAIR', label: 'Trung bình' },
    { value: 'GOOD', label: 'Tốt' },
    { value: 'EXCELLENT', label: 'Rất tốt' },
];

const ITEMS = [
    { name: 'engineRating', label: 'Động cơ' },
    { name: 'brakeRating', label: 'Phanh' },
    { name: 'tireRating', label: 'Lốp' },
    { name: 'chainRating', label: 'Truyền động' },
    { name: 'electricalRating', label: 'Hệ thống điện' },
];

const EMPTY = {
    engineRating: 'GOOD',
    brakeRating: 'GOOD',
    tireRating: 'GOOD',
    chainRating: 'GOOD',
    electricalRating: 'GOOD',
};

/**
 * Form chấm điểm tình trạng xe.
 *
 * condition : dữ liệu đánh giá hiện có (null nếu chưa chấm lần nào)
 * onSaved   : hàm cha truyền xuống để nhận kết quả mới sau khi lưu
 */
export default function ConditionForm({ productId, condition, onSaved }) {
    const [form, setForm] = useState(() => {
        if (condition === null || condition === undefined) return EMPTY;
        return {
            engineRating: condition.engineRating,
            brakeRating: condition.brakeRating,
            tireRating: condition.tireRating,
            chainRating: condition.chainRating,
            electricalRating: condition.electricalRating,
        };
    });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    function handleChange(e) {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        setSaving(true);
        setError('');
        setMessage('');

        client
            .put(`/admin/motorbikes/${productId}/condition`, form)
            .then((res) => {
                onSaved(res.data.motorbike.condition);
                setMessage('Đã lưu kết quả đánh giá.');
            })
            .catch((err) => setError(apiError(err, 'Không lưu được kết quả đánh giá.')))
            .finally(() => setSaving(false));
    }

    const hasResult = condition !== null && condition !== undefined;

    return (
        <section className="ap-section">
            <h2>Đánh giá tình trạng xe</h2>

            {message !== '' && <p className="ap-success">{message}</p>}
            {error !== '' && <p className="ap-error">{error}</p>}

            {hasResult && (
                <div className="ap-verdict">
                    <p>
                        Điểm tình trạng: <strong>{condition.coefficient}</strong> / 10 —{' '}
                        {condition.verdict}
                    </p>
                    {condition.repairAdvice.length > 0 && (
                        <ul>
                            {condition.repairAdvice.map((advice) => (
                                <li key={advice}>{advice}</li>
                            ))}
                        </ul>
                    )}
                </div>
            )}

            <form className="ap-form" onSubmit={handleSubmit}>
                <div className="ap-row">
                    {ITEMS.map((item) => (
                        <div className="ap-field" key={item.name}>
                            <label htmlFor={`cond-${item.name}`}>{item.label} *</label>
                            <select
                                id={`cond-${item.name}`}
                                name={item.name}
                                value={form[item.name]}
                                onChange={handleChange}
                            >
                                {LEVELS.map((level) => (
                                    <option value={level.value} key={level.value}>
                                        {level.label}
                                    </option>
                                ))}
                            </select>
                        </div>
                    ))}
                </div>

                <button type="submit" className="ap-btn ap-btn-primary" disabled={saving}>
                    {saving ? 'Đang lưu…' : hasResult ? 'Cập nhật đánh giá' : 'Lưu đánh giá'}
                </button>
            </form>
        </section>
    );
}
