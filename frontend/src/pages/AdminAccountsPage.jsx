import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import client from '../api/client.js'
import AdminNav from '../components/AdminNav.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import { formatDateTime } from '../utils/format.js'
import './AdminPages.css'
import './AdminAccountsPage.css'

const FILTERS = [
    { value: 'ALL', label: 'Tất cả' },
    { value: 'ACTIVE', label: 'Đang hoạt động' },
    { value: 'LOCKED', label: 'Đã khóa' },
]
const MAX_REASON = 200

/**
 * Admin: tài khoản khách hàng. Tìm theo email, lọc theo trạng thái, khóa và mở khóa.
 * Khóa tài khoản đang có đơn chưa hoàn tất phải được admin xác nhận (force). Bộ lọc, từ khóa và trang nằm trên địa chỉ.
 */
export default function AdminAccountsPage() {
    const { logout } = useAuth()
    const [params, setParams] = useSearchParams()
    const q = (params.get('q') || '').trim()
    const rawStatus = params.get('status')
    const status = FILTERS.some((f) => f.value === rawStatus) ? rawStatus : 'ALL'
    const page = Math.max(Number.parseInt(params.get('page') || '0', 10) || 0, 0)
    const key = `${q}|${status}|${page}`

    const [result, setResult] = useState({ key: null, data: null, error: '' })
    const [panel, setPanel] = useState(null)   // { id, mode: 'lock' | 'unlock' }: dòng đang mở khung xác nhận
    const [reason, setReason] = useState('')
    const [force, setForce] = useState(false)
    const [busy, setBusy] = useState(false)
    const [actionError, setActionError] = useState('')

    useEffect(() => {
        let ignore = false
        client.get('/admin/accounts', { params: { q: q || undefined, status, page, size: 15 } })
            .then((res) => { if (!ignore) setResult({ key, data: res.data, error: '' }) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setResult({ key, data: null, error: apiError(err, 'Không tải được danh sách tài khoản.') })
            })
        return () => { ignore = true }
    }, [q, status, page, key, logout])

    function updateParams(changes) {
        const next = new URLSearchParams(params)
        for (const [name, value] of Object.entries(changes)) {
            if (value) next.set(name, value)
            else next.delete(name)
        }
        setPanel(null)
        setParams(next)
    }

    function search(event) {
        event.preventDefault()
        const value = new FormData(event.currentTarget).get('q')
        updateParams({ q: String(value ?? '').trim(), page: '' })
    }

    function openPanel(account, mode) {
        setPanel({ id: account.accountId, mode })
        setReason('')
        setForce(false)
        setActionError('')
    }

    async function submit(account) {
        setBusy(true)
        setActionError('')
        try {
            const id = encodeURIComponent(account.accountId)
            const res = panel.mode === 'lock'
                ? await client.post(`/admin/accounts/${id}/lock`, { reason: reason.trim() || null, force })
                : await client.post(`/admin/accounts/${id}/unlock`)
            // dòng vừa xử lý giữ nguyên chỗ cũ với trạng thái mới, để admin thấy kết quả
            setResult((r) => r.data
                ? { ...r, data: { ...r.data, items: r.data.items.map((a) => (a.accountId === account.accountId ? res.data : a)) } }
                : r)
            setPanel(null)
        } catch (err) {
            if (err.response?.status === 401) logout()
            else setActionError(apiError(err, 'Không cập nhật được tài khoản.'))
        } finally {
            setBusy(false)
        }
    }

    const loading = result.key !== key
    const data = result.data

    return (
        <section className="admin">
            <h1>Quản trị</h1>
            <AdminNav />

            <div className="accounts__bar">
                <div className="admin__filters" role="tablist" aria-label="Lọc theo trạng thái">
                    {FILTERS.map((f) => (
                        <button key={f.value} type="button"
                                className={status === f.value ? 'admin__filter admin__filter--on' : 'admin__filter'}
                                onClick={() => updateParams({ status: f.value === 'ALL' ? '' : f.value, page: '' })}>{f.label}</button>
                    ))}
                </div>
                <form className="accounts__search" onSubmit={search} role="search">
                    <input key={q} name="q" type="search" defaultValue={q} maxLength={100}
                           placeholder="Tìm theo email" aria-label="Tìm tài khoản" />
                    <button type="submit" className="admin__btn">Tìm</button>
                </form>
            </div>

            {loading && <p className="admin__message">Đang tải...</p>}
            {!loading && result.error && <p className="admin__message admin__message--error">{result.error}</p>}

            {!loading && data && (
                data.items.length === 0 ? (
                    <p className="admin__message">Không có tài khoản nào.</p>
                ) : (
                    <>
                        <div className="admin__table-wrap">
                            <table className="admin__table">
                                <thead>
                                    <tr><th>Email</th><th>Điện thoại</th><th>Trạng thái</th><th>Đơn chưa xong</th><th></th></tr>
                                </thead>
                                <tbody>
                                    {data.items.map((a) => (
                                        <AccountRows key={a.accountId} account={a} panel={panel?.id === a.accountId ? panel : null}
                                                     reason={reason} setReason={setReason} force={force} setForce={setForce}
                                                     busy={busy} actionError={actionError}
                                                     onOpen={openPanel} onCancel={() => setPanel(null)} onSubmit={submit} />
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <div className="admin__pager">
                            <button type="button" disabled={data.page <= 0} onClick={() => updateParams({ page: data.page - 1 > 0 ? String(data.page - 1) : '' })}>← Trước</button>
                            <span>Trang {data.page + 1} / {Math.max(data.totalPages, 1)} · {data.totalItems} tài khoản</span>
                            <button type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => updateParams({ page: String(data.page + 1) })}>Sau →</button>
                        </div>
                    </>
                )
            )}
        </section>
    )
}

/** Một tài khoản: dòng thông tin, và (khi đang khóa/mở khóa) dòng xác nhận ngay bên dưới. */
function AccountRows({ account: a, panel, reason, setReason, force, setForce, busy, actionError, onOpen, onCancel, onSubmit }) {
    const needsConfirm = panel?.mode === 'lock' && a.unfinishedOrders > 0
    return (
        <>
            <tr>
                <td>{a.email}</td>
                <td>{a.phone || '—'}</td>
                <td>
                    {a.locked ? (
                        <>
                            <span className="account-badge account-badge--locked">Đã khóa</span>
                            <span className="admin__sub">
                                {a.lockReason || 'Không ghi lý do'}{a.lockedAt ? ` · ${formatDateTime(a.lockedAt)}` : ''}
                            </span>
                        </>
                    ) : (
                        <span className="account-badge account-badge--active">Đang hoạt động</span>
                    )}
                </td>
                <td>{a.unfinishedOrders}</td>
                <td>
                    {a.locked
                        ? <button type="button" className="admin__btn" disabled={busy} onClick={() => onOpen(a, 'unlock')}>Mở khóa</button>
                        : <button type="button" className="admin__btn admin__btn--danger" disabled={busy} onClick={() => onOpen(a, 'lock')}>Khóa</button>}
                </td>
            </tr>
            {panel && (
                <tr className="accounts__panel-row">
                    <td colSpan={5}>
                        {panel.mode === 'lock' ? (
                            <div className="accounts__panel">
                                <label className="accounts__field">
                                    <span>Lý do khóa (không bắt buộc)</span>
                                    <input type="text" value={reason} maxLength={MAX_REASON} disabled={busy}
                                           onChange={(e) => setReason(e.target.value)} />
                                </label>
                                {a.unfinishedOrders > 0 && (
                                    <label className="accounts__warn">
                                        <input type="checkbox" checked={force} disabled={busy} onChange={(e) => setForce(e.target.checked)} />
                                        <span>Tài khoản đang có {a.unfinishedOrders} đơn chưa hoàn tất. Tôi đã xem cảnh báo và vẫn muốn khóa.</span>
                                    </label>
                                )}
                                <div className="accounts__panel-actions">
                                    <button type="button" className="admin__btn admin__btn--danger" disabled={busy || (needsConfirm && !force)}
                                            onClick={() => onSubmit(a)}>Xác nhận khóa</button>
                                    <button type="button" className="admin__btn" disabled={busy} onClick={onCancel}>Hủy</button>
                                </div>
                            </div>
                        ) : (
                            <div className="accounts__panel">
                                <p className="accounts__ask">Mở khóa cho {a.email}? Khách sẽ đăng nhập lại được.</p>
                                <div className="accounts__panel-actions">
                                    <button type="button" className="admin__btn admin__btn--primary" disabled={busy} onClick={() => onSubmit(a)}>Xác nhận mở khóa</button>
                                    <button type="button" className="admin__btn" disabled={busy} onClick={onCancel}>Hủy</button>
                                </div>
                            </div>
                        )}
                        {actionError && <p className="admin__message admin__message--error" role="alert">{actionError}</p>}
                    </td>
                </tr>
            )}
        </>
    )
}
