import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client.js'
import VehicleModelPicker from '../components/VehicleModelPicker.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import './MyVehiclesPage.css'

const MAX_NICKNAME = 50

/**
 * Xe của tôi (hồ sơ xe): khách lưu các xe mình đang có (dòng xe, năm đăng ký, biệt danh).
 * Chat box dùng hồ sơ để gợi ý phụ tùng lắp vừa xe mà không bắt khách nói lại xe mỗi lần.
 */
export default function MyVehiclesPage() {
    const { logout } = useAuth()
    const [vehicles, setVehicles] = useState(null)   // null = đang tải
    const [loadError, setLoadError] = useState('')
    const [editingId, setEditingId] = useState(null)  // hồ sơ đang sửa
    const [deletingId, setDeletingId] = useState(null) // hồ sơ đang chờ xác nhận xóa
    const [actionError, setActionError] = useState('')
    const [busy, setBusy] = useState(false)
    const [addFormKey, setAddFormKey] = useState(0)   // đổi key để biểu mẫu thêm xe xóa trắng sau khi lưu

    useEffect(() => {
        let ignore = false
        client.get('/vehicle-profiles')
            .then((res) => { if (!ignore) setVehicles(res.data) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setLoadError(apiError(err, 'Không tải được danh sách xe.'))
            })
        return () => { ignore = true }
    }, [logout])

    /** Chạy một thao tác ghi: 401 thì đăng xuất, lỗi khác hiện câu báo lỗi của backend. Trả true nếu thành công. */
    async function run(action) {
        setBusy(true)
        setActionError('')
        try {
            await action()
            return true
        } catch (err) {
            if (err.response?.status === 401) logout()
            else setActionError(apiError(err, 'Không lưu được thay đổi.'))
            return false
        } finally {
            setBusy(false)
        }
    }

    async function add(values) {
        const ok = await run(async () => {
            const res = await client.post('/vehicle-profiles', values)
            setVehicles((list) => [res.data, ...(list ?? [])])
        })
        if (ok) setAddFormKey((k) => k + 1)
    }

    async function save(id, values) {
        const ok = await run(async () => {
            const res = await client.put(`/vehicle-profiles/${encodeURIComponent(id)}`, values)
            setVehicles((list) => list.map((v) => (v.profileId === id ? res.data : v)))
        })
        if (ok) setEditingId(null)
    }

    async function remove(id) {
        const ok = await run(async () => {
            await client.delete(`/vehicle-profiles/${encodeURIComponent(id)}`)
            setVehicles((list) => list.filter((v) => v.profileId !== id))
        })
        if (ok) setDeletingId(null)
    }

    return (
        <section className="vehicles">
            <h1>Xe của tôi</h1>
            <p className="vehicles__intro">
                Lưu xe bạn đang chạy để trợ lý gợi ý phụ tùng lắp vừa xe đó. Bạn có thể lưu tối đa 5 xe.
            </p>

            {loadError && <p className="vehicles__message vehicles__message--error">{loadError}</p>}
            {vehicles === null && !loadError && <p className="vehicles__message">Đang tải...</p>}
            {actionError && <p className="vehicles__message vehicles__message--error" role="alert">{actionError}</p>}

            {vehicles && (
                <>
                    <VehicleForm key={addFormKey} title="Thêm xe" submitLabel="Lưu xe" busy={busy} requireVehicle
                                 onSubmit={add} />

                    {vehicles.length === 0 ? (
                        <p className="vehicles__message">Bạn chưa lưu xe nào.</p>
                    ) : (
                        <ul className="vehicles__list">
                            {vehicles.map((v) => (
                                <li key={v.profileId} className="vehicle-card">
                                    {editingId === v.profileId ? (
                                        <VehicleForm title="Sửa xe" submitLabel="Lưu thay đổi" busy={busy} current={v}
                                                     onSubmit={(values) => save(v.profileId, values)}
                                                     onCancel={() => { setEditingId(null); setActionError('') }} />
                                    ) : (
                                        <>
                                            <div className="vehicle-card__info">
                                                <strong>{v.nickname || `${v.brand} ${v.model}`}</strong>
                                                <span>{v.brand} {v.model} · đăng ký năm {v.registrationYear}</span>
                                            </div>
                                            {deletingId === v.profileId ? (
                                                <div className="vehicle-card__actions">
                                                    <span className="vehicle-card__ask">Xóa xe này?</span>
                                                    <button type="button" className="vehicles__btn vehicles__btn--danger" disabled={busy}
                                                            onClick={() => remove(v.profileId)}>Xóa</button>
                                                    <button type="button" className="vehicles__btn" disabled={busy}
                                                            onClick={() => setDeletingId(null)}>Hủy</button>
                                                </div>
                                            ) : (
                                                <div className="vehicle-card__actions">
                                                    <Link className="vehicles__btn" to={`/chat?vehicle=${encodeURIComponent(v.profileId)}`}>Hỏi AI về xe này</Link>
                                                    <button type="button" className="vehicles__btn" disabled={busy}
                                                            onClick={() => { setEditingId(v.profileId); setDeletingId(null); setActionError('') }}>Sửa</button>
                                                    <button type="button" className="vehicles__btn" disabled={busy}
                                                            onClick={() => { setDeletingId(v.profileId); setEditingId(null); setActionError('') }}>Xóa</button>
                                                </div>
                                            )}
                                        </>
                                    )}
                                </li>
                            ))}
                        </ul>
                    )}
                </>
            )}
        </section>
    )
}

/**
 * Biểu mẫu thêm hoặc sửa một xe. Thêm: bắt buộc chọn đủ hãng, dòng, năm. Sửa (current): giữ xe cũ trừ khi tick "Đổi dòng xe".
 * onSubmit nhận { nickname, vehicleModelId, registrationYear }; nơi gọi lo việc gửi lên máy chủ.
 */
function VehicleForm({ title, submitLabel, busy, current, requireVehicle = false, onSubmit, onCancel }) {
    const [nickname, setNickname] = useState(current?.nickname ?? '')
    const [changeVehicle, setChangeVehicle] = useState(requireVehicle)
    const [picked, setPicked] = useState(null)   // { modelId, year } hoặc null khi chọn chưa đủ

    const needPick = requireVehicle || changeVehicle
    const canSubmit = !busy && nickname.trim() !== '' && (!needPick || picked !== null)   // biệt danh bắt buộc (SRS UC-15)

    function submit(event) {
        event.preventDefault()
        if (!canSubmit) return
        onSubmit({
            nickname: nickname.trim(),
            vehicleModelId: needPick ? picked.modelId : current.vehicleModelId,
            registrationYear: needPick ? picked.year : current.registrationYear,
        })
    }

    return (
        <form className="vehicle-form" onSubmit={submit}>
            <h2>{title}</h2>
            <label className="vehicle-form__field">
                <span>Biệt danh</span>
                <input type="text" value={nickname} maxLength={MAX_NICKNAME} placeholder="Ví dụ: Xe đi làm" disabled={busy} required
                       onChange={(e) => setNickname(e.target.value)} />
            </label>

            {current && (
                <label className="vehicle-form__check">
                    <input type="checkbox" checked={changeVehicle} disabled={busy}
                           onChange={(e) => { setChangeVehicle(e.target.checked); setPicked(null) }} />
                    <span>Đổi dòng xe (hiện tại: {current.brand} {current.model}, năm {current.registrationYear})</span>
                </label>
            )}

            {needPick && (
                <div className="vehicle-form__field">
                    <span>Dòng xe và năm đăng ký</span>
                    <VehicleModelPicker onChange={setPicked} />
                </div>
            )}

            <div className="vehicle-form__actions">
                <button type="submit" className="vehicles__btn vehicles__btn--primary" disabled={!canSubmit}>{submitLabel}</button>
                {onCancel && <button type="button" className="vehicles__btn" disabled={busy} onClick={onCancel}>Hủy</button>}
            </div>
        </form>
    )
}
