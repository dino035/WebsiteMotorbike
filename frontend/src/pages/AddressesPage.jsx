import { useCallback, useEffect, useState } from 'react'
import client from '../api/client.js'
import AddressForm from '../components/AddressForm.jsx'
import useAuth from '../hooks/useAuth.js'
import { apiError } from '../utils/apiError.js'
import './AddressesPage.css'

const MAX_ADDRESSES = 10   // khớp AddressService.MAX_PER_ACCOUNT ở backend

export default function AddressesPage() {
    const { logout } = useAuth()
    const [addresses, setAddresses] = useState(null)
    const [loadError, setLoadError] = useState('')
    const [actionError, setActionError] = useState('')
    const [adding, setAdding] = useState(false)
    const [editingId, setEditingId] = useState(null)
    const [busyId, setBusyId] = useState(null)

    // Token hết hạn hoặc tài khoản bị khóa thì đăng xuất để khách đăng nhập lại.
    const guard = useCallback((err) => {
        if (err.response?.status === 401) logout()
        throw err
    }, [logout])

    const load = useCallback(
        () => client.get('/addresses').then((res) => { setAddresses(res.data); return res.data }).catch(guard),
        [guard],
    )

    useEffect(() => {
        let ignore = false
        client.get('/addresses')
            .then((res) => { if (!ignore) setAddresses(res.data) })
            .catch((err) => {
                if (ignore) return
                if (err.response?.status === 401) logout()
                else setLoadError(apiError(err, 'Không tải được sổ địa chỉ.'))
            })
        return () => { ignore = true }
    }, [logout])

    // Thêm/sửa/đặt mặc định/xóa đều có thể đổi cờ "mặc định" của địa chỉ khác nên tải lại cả danh sách.
    async function create(values) {
        await client.post('/addresses', values).catch(guard)
        await load()
        setAdding(false)
    }

    async function update(addressId, values) {
        await client.put(`/addresses/${encodeURIComponent(addressId)}`, values).catch(guard)
        await load()
        setEditingId(null)
    }

    async function runAction(addressId, request) {
        setBusyId(addressId)
        setActionError('')
        try {
            await request().catch(guard)
            await load()
        } catch (err) {
            setActionError(apiError(err))
        } finally {
            setBusyId(null)
        }
    }

    const setDefault = (a) => runAction(a.addressId, () => client.post(`/addresses/${encodeURIComponent(a.addressId)}/default`))

    function remove(a) {
        if (!window.confirm(`Xóa địa chỉ của ${a.receiverName}?`)) return
        runAction(a.addressId, () => client.delete(`/addresses/${encodeURIComponent(a.addressId)}`))
    }

    if (loadError) return <p className="addresses__message addresses__message--error">{loadError}</p>
    if (!addresses) return <p className="addresses__message">Đang tải sổ địa chỉ...</p>

    const full = addresses.length >= MAX_ADDRESSES

    return (
        <section className="addresses">
            <div className="addresses__head">
                <h1>Sổ địa chỉ</h1>
                {!adding && (
                    <button type="button" className="addresses__add" disabled={full}
                            title={full ? `Tối đa ${MAX_ADDRESSES} địa chỉ` : ''}
                            onClick={() => { setAdding(true); setEditingId(null) }}>
                        + Thêm địa chỉ
                    </button>
                )}
            </div>
            {full && <p className="addresses__hint">Bạn đã lưu tối đa {MAX_ADDRESSES} địa chỉ. Hãy xóa bớt để thêm mới.</p>}
            {actionError && <p className="addresses__message addresses__message--error" role="alert">{actionError}</p>}

            {adding && (
                <div className="address-card address-card--form">
                    <h2>Địa chỉ mới</h2>
                    <AddressForm forceDefault={addresses.length === 0} submitLabel="Lưu địa chỉ"
                                 onSubmit={create} onCancel={() => setAdding(false)} />
                </div>
            )}

            {addresses.length === 0 && !adding && (
                <p className="addresses__message">Bạn chưa lưu địa chỉ nào. Thêm một địa chỉ để đặt hàng nhanh hơn.</p>
            )}

            <ul className="addresses__list">
                {addresses.map((a) => (
                    <li key={a.addressId} className="address-card">
                        {editingId === a.addressId ? (
                            <>
                                <h2>Sửa địa chỉ</h2>
                                <AddressForm initial={{ ...a, makeDefault: a.defaultAddress }}
                                             forceDefault={a.defaultAddress} submitLabel="Lưu thay đổi"
                                             onSubmit={(values) => update(a.addressId, values)}
                                             onCancel={() => setEditingId(null)} />
                            </>
                        ) : (
                            <>
                                <div className="address-card__body">
                                    <p className="address-card__name">
                                        {a.receiverName} <span className="address-card__phone">· {a.receiverPhone}</span>
                                        {a.defaultAddress && <span className="address-card__badge">Mặc định</span>}
                                    </p>
                                    <p className="address-card__full">{a.fullAddress}</p>
                                </div>
                                <div className="address-card__actions">
                                    <button type="button" disabled={busyId === a.addressId}
                                            onClick={() => { setEditingId(a.addressId); setAdding(false) }}>Sửa</button>
                                    {!a.defaultAddress && (
                                        <button type="button" disabled={busyId === a.addressId}
                                                onClick={() => setDefault(a)}>Đặt làm mặc định</button>
                                    )}
                                    <button type="button" className="address-card__delete" disabled={busyId === a.addressId}
                                            onClick={() => remove(a)}>Xóa</button>
                                </div>
                            </>
                        )}
                    </li>
                ))}
            </ul>
        </section>
    )
}
