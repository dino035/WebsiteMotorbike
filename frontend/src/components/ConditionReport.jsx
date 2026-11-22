import './ConditionReport.css'

/**
 * Bảng đánh giá tình trạng xe (UC-18) hiện cho khách xem trên trang chi tiết.
 * API trả về tên hằng số của RatingLevel ("GOOD"), không trả về nhãn tiếng Việt, nên đổi ở đây.
 */
const LEVELS = {
    POOR: { label: 'Kém', tone: 'poor' },
    FAIR: { label: 'Trung bình', tone: 'fair' },
    GOOD: { label: 'Tốt', tone: 'good' },
    EXCELLENT: { label: 'Rất tốt', tone: 'excellent' },
}

const ITEMS = [
    ['engineRating', 'Động cơ'],
    ['brakeRating', 'Phanh'],
    ['tireRating', 'Lốp'],
    ['chainRating', 'Nhông xích'],
    ['electricalRating', 'Điện'],
]

export default function ConditionReport({ condition }) {
    if (!condition) return null

    const advice = condition.repairAdvice ?? []

    return (
        <section className="condition">
            <div className="condition__head">
                <h2>Tình trạng xe</h2>
                {condition.coefficient != null && (
                    <span className="condition__coef" title="Trung bình 5 hạng mục, quy về thang 10">
                        Điểm tình trạng: <strong>{condition.coefficient}</strong>/10
                    </span>
                )}
            </div>

            <ul className="condition__items">
                {ITEMS.map(([key, label]) => {
                    const level = LEVELS[condition[key]]
                    return (
                        <li key={key} className="condition__item">
                            <span className="condition__item-label">{label}</span>
                            <span className={`condition__level condition__level--${level ? level.tone : 'none'}`}>
                                {level ? level.label : 'Chưa chấm'}
                            </span>
                        </li>
                    )
                })}
            </ul>

            {condition.verdict && <p className="condition__verdict">{condition.verdict}</p>}

            {advice.length > 0 && (
                <div className="condition__advice">
                    <h3>Nên kiểm tra thêm</h3>
                    <ul>
                        {advice.map((line) => <li key={line}>{line}</li>)}
                    </ul>
                </div>
            )}
        </section>
    )
}
