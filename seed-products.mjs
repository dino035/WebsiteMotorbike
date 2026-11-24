/**
 * Tạo hàng loạt phụ tùng qua API, thay cho việc nhập tay từng sản phẩm ở tab Quản trị.
 *
 * Chạy y như seed-compatibility.mjs:
 *
 *   $env:API_URL   = "https://motoparts-api-vjn0.onrender.com/api"
 *   $env:ADMIN_EMAIL    = "email admin"
 *   $env:ADMIN_PASSWORD = "mật khẩu admin"
 *   node seed-products.mjs --dry      (xem trước, không ghi gì)
 *   node seed-products.mjs            (chạy thật)
 *
 * Chạy lại được: sản phẩm đã có cùng tên thì bỏ qua.
 *
 * ẢNH KHÔNG TẠO ĐƯỢC BẰNG SCRIPT. Endpoint ảnh nhận multipart và cần file thật,
 * nên sau khi chạy xong vẫn phải vào tab Sản phẩm tải ảnh lên cho từng món.
 *
 * Giá ở đây là mức tham khảo tôi tra trên thị trường hoặc ước theo mặt bằng.
 * Sửa thẳng trong mảng PRODUCTS bên dưới trước khi chạy nếu bạn muốn giá khác.
 */

const API = process.env.API_URL || 'http://localhost:8080/api'
const EMAIL = process.env.ADMIN_EMAIL
const PASSWORD = process.env.ADMIN_PASSWORD
const DRY_RUN = process.argv.includes('--dry')

// status:     AVAILABLE (Có sẵn) | SOLD (Hết hàng)
// partOrigin: GENUINE (Chính hãng) | AFTERMARKET (Phụ tùng bên thứ 3)
const PRODUCTS = [
    // Nhớt
    { name: 'Nhớt Mobil Super Moto Scooter 10W-40 0.8L', price: 89000, partType: 'Nhớt', partOrigin: 'AFTERMARKET' },
    { name: 'Nhớt Quatex 4T 20W-50 1L', price: 74000, partType: 'Nhớt', partOrigin: 'AFTERMARKET' },
    { name: 'Nhớt Kixx Ultra 4T 10W-40 1L', price: 95000, partType: 'Nhớt', partOrigin: 'AFTERMARKET' },
    { name: 'Nhớt Motul 3100 Gold 4T 10W-40 1L', price: 145000, partType: 'Nhớt', partOrigin: 'AFTERMARKET' },

    // Bugi
    { name: 'Bugi NGK C7HSA chân ngắn', price: 15000, partType: 'Bugi', partOrigin: 'GENUINE' },
    { name: 'Bugi NGK CPR7EA-9 chân dài', price: 85000, partType: 'Bugi', partOrigin: 'GENUINE' },
    { name: 'Bugi NGK Iridium CPR8EAIX-9', price: 245000, partType: 'Bugi', partOrigin: 'GENUINE' },

    // Ắc quy
    { name: 'Ắc quy Globe WP5S-3BP 12V-5Ah', price: 350000, partType: 'Ắc quy', partOrigin: 'AFTERMARKET' },
    { name: 'Ắc quy GS GT7A-H 12V-7Ah', price: 560000, partType: 'Ắc quy', partOrigin: 'GENUINE' },
    { name: 'Ắc quy GS GT6A 12V-6Ah', price: 420000, partType: 'Ắc quy', partOrigin: 'GENUINE' },

    // Lốp
    { name: 'Lốp Casumina 80/90-14', price: 320000, partType: 'Lốp', partOrigin: 'AFTERMARKET' },
    { name: 'Lốp Michelin City Grip 2 80/90-14', price: 780000, partType: 'Lốp', partOrigin: 'AFTERMARKET' },
    { name: 'Lốp IRC NR77 70/90-17', price: 250000, partType: 'Lốp', partOrigin: 'GENUINE' },
    { name: 'Lốp Michelin Pilot Street 70/90-17', price: 520000, partType: 'Lốp', partOrigin: 'AFTERMARKET' },

    // Phanh
    { name: 'Má phanh đĩa phổ thông', price: 35000, partType: 'Phanh', partOrigin: 'AFTERMARKET' },
    { name: 'Má phanh đĩa Nissin', price: 150000, partType: 'Phanh', partOrigin: 'GENUINE' },
    { name: 'Má phanh đĩa Brembo', price: 450000, partType: 'Phanh', partOrigin: 'AFTERMARKET' },

    // Nhông xích
    { name: 'Bộ nhông sên dĩa phổ thông', price: 150000, partType: 'Nhông xích', partOrigin: 'AFTERMARKET' },
    { name: 'Bộ nhông dĩa Recto + sên RK 428', price: 430000, partType: 'Nhông xích', partOrigin: 'AFTERMARKET' },

    // Nước làm mát
    { name: 'Nước làm mát Motul Motocool Expert', price: 165000, partType: 'Nước làm mát', partOrigin: 'AFTERMARKET' },

    // Dầu phanh
    { name: 'Dầu phanh DOT 3 phổ thông', price: 45000, partType: 'Dầu phanh', partOrigin: 'AFTERMARKET' },
    { name: 'Dầu phanh Motul DOT 4', price: 120000, partType: 'Dầu phanh', partOrigin: 'AFTERMARKET' },
    { name: 'Dầu phanh Motul RBF 600', price: 350000, partType: 'Dầu phanh', partOrigin: 'AFTERMARKET' },

    // Lọc gió
    { name: 'Lọc gió xe số Wave / Future', price: 60000, partType: 'Lọc gió', partOrigin: 'GENUINE' },
    { name: 'Lọc gió Honda Vision / Lead chính hãng', price: 120000, partType: 'Lọc gió', partOrigin: 'GENUINE' },
]

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
    if (!res.ok) {
        const err = new Error(data?.message || `HTTP ${res.status}`)
        err.status = res.status
        throw err
    }
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

    console.log(`Máy chủ: ${API}`)
    if (DRY_RUN) console.log('CHẾ ĐỘ THỬ: không ghi gì vào cơ sở dữ liệu.\n')

    console.log('Đang đăng nhập (máy chủ ngủ thì bước này mất vài phút)...')
    const auth = await call('/auth/login', { method: 'POST', body: { email: EMAIL, password: PASSWORD } })
    const token = auth.token
    console.log(`Đăng nhập xong: ${auth.user?.email ?? EMAIL}\n`)

    // Đọc kho hiện có để không tạo trùng tên (backend ép size tối đa 50).
    const existing = new Set()
    for (let pageNo = 0; ; pageNo++) {
        const page = await call(`/products?type=sparepart&size=50&page=${pageNo}`, { token })
        for (const p of page.items ?? []) existing.add(p.name.trim().toLowerCase())
        if (pageNo + 1 >= (page.totalPages ?? 1)) break
    }
    console.log(`Kho đang có ${existing.size} phụ tùng.\n`)

    let created = 0
    let skipped = 0
    let failed = 0

    for (const p of PRODUCTS) {
        if (existing.has(p.name.trim().toLowerCase())) {
            skipped++
            console.log(`đã có   ${p.name}`)
            continue
        }
        if (DRY_RUN) {
            console.log(`sẽ tạo  ${p.name}  —  ${p.price.toLocaleString('vi-VN')} đ  (${p.partType}, ${p.partOrigin}, Hết hàng)`)
            continue
        }
        try {
            await call('/admin/spare-parts', {
                method: 'POST',
                token,
                body: {
                    name: p.name,
                    price: p.price,
                    // Backend không cho đặt Có sẵn khi phụ tùng chưa có ảnh, nên tạo ở
                    // tình trạng Hết hàng. Thêm ảnh xong mới chuyển sang Có sẵn.
                    status: 'SOLD',
                    partType: p.partType,
                    partOrigin: p.partOrigin,
                },
            })
            created++
            console.log(`TẠO     ${p.name}`)
        } catch (e) {
            failed++
            console.log(`LỖI     ${p.name} — ${e.message}`)
        }
        await sleep(150)
    }

    console.log('\n' + '─'.repeat(50))
    console.log(`Đã tạo:   ${created}`)
    console.log(`Đã có:    ${skipped}`)
    console.log(`Lỗi:      ${failed}`)
    if (!DRY_RUN && created > 0) {
        console.log('\nSản phẩm được tạo ở tình trạng HẾT HÀNG vì chưa có ảnh (backend bắt buộc).')
        console.log('Bước tiếp:')
        console.log('  1. node seed-compatibility.mjs   — tạo luật tương thích cho sản phẩm mới')
        console.log('  2. Vào tab Sản phẩm: tải ảnh cho từng món, rồi chuyển sang Có sẵn')
        console.log('     (chưa chuyển thì trợ lý và trang sản phẩm vẫn chưa hiện chúng)')
    }
}

main().catch((e) => {
    console.error('\nDừng vì lỗi:', e.message)
    process.exit(1)
})
