/**
 * Tải ảnh hàng loạt cho phụ tùng, rồi tự chuyển sang tình trạng Có sẵn.
 *
 * CHUẨN BỊ:
 *   1. Tạo thư mục  anh-phu-tung  ở cạnh file này.
 *   2. Bỏ ảnh vào đó, đặt tên file theo tên sản phẩm. Không cần giống hệt:
 *      script bỏ dấu, bỏ hoa thường, bỏ ký tự lạ rồi mới so.
 *
 *      Ví dụ, sản phẩm "Bugi NGK CPR7EA-9 chân dài" khớp với:
 *         bugi-ngk-cpr7ea-9-chan-dai.jpg
 *         Bugi NGK CPR7EA-9 chan dai.png
 *         bugi ngk cpr7ea 9.webp          (khớp một phần, vẫn nhận nếu không mơ hồ)
 *
 *      Nhiều ảnh cho một sản phẩm: thêm -1, -2 ở cuối tên file.
 *
 * CHẠY:
 *   $env:API_URL   = "https://motoparts-api-vjn0.onrender.com/api"
 *   $env:ADMIN_EMAIL    = "email admin"
 *   $env:ADMIN_PASSWORD = "mật khẩu admin"
 *   node seed-photos.mjs --dry     (chỉ đối chiếu tên, không tải gì)
 *   node seed-photos.mjs           (tải thật)
 *
 * Thêm --giu-het-hang nếu muốn tải ảnh mà chưa bật Có sẵn.
 */

import { readdirSync, readFileSync } from 'node:fs'
import { extname, basename, join } from 'node:path'

const API = process.env.API_URL || 'http://localhost:8080/api'
const EMAIL = process.env.ADMIN_EMAIL
const PASSWORD = process.env.ADMIN_PASSWORD
const DRY_RUN = process.argv.includes('--dry')
const GIU_HET_HANG = process.argv.includes('--giu-het-hang')

const THU_MUC = 'anh-phu-tung'
const DUOI_ANH = ['.jpg', '.jpeg', '.png', '.webp']

const KIEU_MIME = {
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.png': 'image/png',
    '.webp': 'image/webp',
}

/** Bỏ dấu, bỏ hoa thường, gom mọi ký tự không phải chữ/số thành một dấu cách. */
function chuanHoa(s) {
    return s
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')   // bỏ dấu thanh
        .replace(/đ/gi, 'd')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, ' ')
        .trim()
}

/** Bỏ hậu tố -1, -2 ở cuối tên file (dùng cho sản phẩm có nhiều ảnh). */
function boSoThuTu(s) {
    return s.replace(/\s+\d+$/, '').trim()
}

async function call(path, { method = 'GET', token, body } = {}) {
    const res = await fetch(API + path, {
        method,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
    })
    const text = await res.text()
    let data = null
    try { data = text ? JSON.parse(text) : null } catch { data = text }
    if (!res.ok) throw new Error(data?.message || `HTTP ${res.status}`)
    return data
}

async function uploadAnh(productId, duongDan, token) {
    const duoi = extname(duongDan).toLowerCase()
    const buf = readFileSync(duongDan)
    const form = new FormData()
    form.append('file', new Blob([buf], { type: KIEU_MIME[duoi] || 'application/octet-stream' }),
        basename(duongDan))

    const res = await fetch(`${API}/admin/spare-parts/${encodeURIComponent(productId)}/photos`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },   // KHÔNG đặt Content-Type: fetch tự thêm boundary
        body: form,
    })
    const text = await res.text()
    let data = null
    try { data = text ? JSON.parse(text) : null } catch { data = text }
    if (!res.ok) throw new Error(data?.message || `HTTP ${res.status}`)
    return data
}

function sleep(ms) {
    return new Promise((r) => setTimeout(r, ms))
}

async function main() {
    if (!EMAIL || !PASSWORD) {
        console.error('Thiếu ADMIN_EMAIL hoặc ADMIN_PASSWORD. Xem hướng dẫn ở đầu file.')
        process.exit(1)
    }

    let tepAnh
    try {
        tepAnh = readdirSync(THU_MUC).filter((f) => DUOI_ANH.includes(extname(f).toLowerCase()))
    } catch {
        console.error(`Không thấy thư mục "${THU_MUC}". Tạo nó cạnh file này rồi bỏ ảnh vào.`)
        process.exit(1)
    }
    if (tepAnh.length === 0) {
        console.error(`Thư mục "${THU_MUC}" chưa có ảnh nào (chấp nhận ${DUOI_ANH.join(', ')}).`)
        process.exit(1)
    }

    console.log(`Máy chủ: ${API}`)
    console.log(`Thư mục ảnh: ${THU_MUC} — ${tepAnh.length} tệp`)
    if (DRY_RUN) console.log('CHẾ ĐỘ THỬ: chỉ đối chiếu tên, không tải gì.')
    console.log('')

    console.log('Đang đăng nhập (máy chủ ngủ thì bước này mất vài phút)...')
    const auth = await call('/auth/login', { method: 'POST', body: { email: EMAIL, password: PASSWORD } })
    const token = auth.token
    console.log(`Đăng nhập xong: ${auth.user?.email ?? EMAIL}\n`)

    const parts = []
    for (let pageNo = 0; ; pageNo++) {
        const page = await call(`/products?type=sparepart&size=50&page=${pageNo}`, { token })
        parts.push(...(page.items ?? []))
        if (pageNo + 1 >= (page.totalPages ?? 1)) break
    }
    console.log(`Kho có ${parts.length} phụ tùng.\n`)

    const chiMuc = parts.map((p) => ({ ...p, khoa: chuanHoa(p.name) }))

    let daTai = 0
    let loi = 0
    const khongKhop = []
    const spCoAnhMoi = new Set()

    for (const tep of tepAnh) {
        const khoaTep = boSoThuTu(chuanHoa(basename(tep, extname(tep))))

        let khop = chiMuc.filter((p) => p.khoa === khoaTep)
        if (khop.length === 0) {
            khop = chiMuc.filter((p) => p.khoa.includes(khoaTep) || khoaTep.includes(p.khoa))
        }

        if (khop.length === 0) {
            khongKhop.push(tep)
            console.log(`KHÔNG KHỚP  ${tep}`)
            continue
        }
        if (khop.length > 1) {
            console.log(`MƠ HỒ       ${tep} khớp ${khop.length} sản phẩm:`)
            khop.forEach((p) => console.log(`            - ${p.name}`))
            console.log('            Đặt tên tệp cụ thể hơn rồi chạy lại.')
            continue
        }

        const sp = khop[0]
        if (DRY_RUN) {
            console.log(`sẽ tải      ${tep}  →  ${sp.name}`)
            spCoAnhMoi.add(sp.productId)
            continue
        }
        try {
            await uploadAnh(sp.productId, join(THU_MUC, tep), token)
            daTai++
            spCoAnhMoi.add(sp.productId)
            console.log(`TẢI LÊN     ${tep}  →  ${sp.name}`)
        } catch (e) {
            loi++
            console.log(`LỖI         ${tep} — ${e.message}`)
        }
        await sleep(200)
    }

    // Bật Có sẵn cho những sản phẩm vừa có ảnh
    let daBat = 0
    if (!DRY_RUN && !GIU_HET_HANG) {
        console.log('\nChuyển sang Có sẵn...')
        for (const id of spCoAnhMoi) {
            try {
                const ct = await call(`/products/${encodeURIComponent(id)}`, { token })
                if (ct.status === 'AVAILABLE') continue
                if (!ct.photos || ct.photos.length === 0) continue
                await call(`/admin/spare-parts/${encodeURIComponent(id)}`, {
                    method: 'PUT',
                    token,
                    body: {
                        name: ct.name,
                        price: ct.price,
                        status: 'AVAILABLE',
                        partType: ct.sparePart?.partType,
                        partOrigin: ct.sparePart?.partOrigin,
                    },
                })
                daBat++
                console.log(`   Có sẵn: ${ct.name}`)
            } catch (e) {
                loi++
                console.log(`   LỖI bật Có sẵn (${id}): ${e.message}`)
            }
            await sleep(150)
        }
    }

    console.log('\n' + '─'.repeat(50))
    console.log(`Ảnh đã tải:     ${daTai}`)
    console.log(`Bật Có sẵn:     ${daBat}`)
    console.log(`Lỗi:            ${loi}`)
    if (khongKhop.length) {
        console.log(`\nKhông khớp sản phẩm nào, đổi tên tệp rồi chạy lại:`)
        khongKhop.forEach((f) => console.log(`  - ${f}`))
    }
}

main().catch((e) => {
    console.error('\nDừng vì lỗi:', e.message)
    process.exit(1)
})
