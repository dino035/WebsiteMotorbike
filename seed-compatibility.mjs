/**
 * Tạo hàng loạt luật tương thích phụ tùng qua API, thay cho việc bấm tay từng luật.
 *
 * Cách chạy (PowerShell, đứng ở thư mục chứa file này):
 *
 *   $env:API_URL   = "https://motoparts-api-vjn0.onrender.com/api"
 *   $env:ADMIN_EMAIL    = "email admin của bạn"
 *   $env:ADMIN_PASSWORD = "mật khẩu admin"
 *   node seed-compatibility.mjs
 *
 * Mật khẩu đi qua biến môi trường chứ không nằm trong file, nên file này
 * commit lên Git vẫn an toàn.
 *
 * Chạy lại nhiều lần được: backend bỏ qua luật đã có (skipped), không báo lỗi.
 * Thêm --dry để chỉ in ra xem sẽ gọi những gì, không ghi gì vào cơ sở dữ liệu.
 */

const API = process.env.API_URL || 'http://localhost:8080/api'
const EMAIL = process.env.ADMIN_EMAIL
const PASSWORD = process.env.ADMIN_PASSWORD
const DRY_RUN = process.argv.includes('--dry')

// Khoảng năm rộng: service chỉ khớp những năm thực sự có trong bảng vehicle_model,
// nên không tạo thừa và không cần biết từng dòng xe ra đời năm nào.
const YEAR_FROM = 2000
const YEAR_TO = 2030

// ---------------------------------------------------------------- nhóm xe

const XE_GA = [
    ['Honda', 'Air Blade'], ['Honda', 'Lead'], ['Honda', 'SH'], ['Honda', 'SH Mode'],
    ['Honda', 'Vario'], ['Honda', 'Vision'],
    ['Yamaha', 'Grande'], ['Yamaha', 'Janus'], ['Yamaha', 'Latte'], ['Yamaha', 'NVX'],
    ['SYM', 'Attila'], ['SYM', 'Elegant'],
    ['Piaggio', 'Liberty'], ['Piaggio', 'Vespa Sprint'],
]

const XE_SO = [
    ['Honda', 'Blade'], ['Honda', 'Future'], ['Honda', 'Wave Alpha'], ['Honda', 'Wave RSX'],
    ['Yamaha', 'Jupiter'], ['Yamaha', 'Sirius'],
]

const CON_TAY = [
    ['Honda', 'Winner X'], ['Yamaha', 'Exciter'], ['Suzuki', 'Raider'], ['Suzuki', 'Satria'],
]

const MOI_XE = [...XE_GA, ...XE_SO, ...CON_TAY]

// Xe làm mát bằng nước (mới cần nước làm mát). Bốn dòng chưa xác minh
// (Attila, Elegant, Liberty, Vespa Sprint) cố ý không đưa vào đây.
const KET_NUOC = [
    ['Honda', 'SH'], ['Honda', 'Vario'], ['Yamaha', 'NVX'],
    ['Honda', 'Winner X'], ['Yamaha', 'Exciter'], ['Suzuki', 'Satria'],
]

// ---------------------------------------------------------------- bảng luật
// `part` là một mẩu tên sản phẩm, khớp không phân biệt hoa thường.
// Sản phẩm chưa có trong kho sẽ được báo "KHÔNG TÌM THẤY" và bỏ qua.

const RULES = [
    // Nhớt xe ga
    { part: 'Mobil Super Moto', xe: XE_GA },
    { part: 'Castrol Activ Scooter', xe: XE_GA },
    { part: 'Motul Scooter Power', xe: XE_GA },

    // Nhớt xe số / côn tay
    { part: 'Quatex', xe: [...XE_SO, ...CON_TAY] },
    { part: 'Kixx Ultra', xe: [...XE_SO, ...CON_TAY] },
    { part: 'Motul 3100', xe: [...XE_SO, ...CON_TAY] },

    // Bugi
    { part: 'C7HSA', xe: [['Honda', 'Wave Alpha'], ['Yamaha', 'Sirius'], ['Yamaha', 'Jupiter']] },
    {
        part: 'CPR7EA-9', xe: [['Honda', 'Air Blade'], ['Honda', 'Lead'], ['Honda', 'SH Mode'],
            ['Honda', 'Vision'], ['Honda', 'Future']],
    },
    { part: 'CPR8EA-9', xe: CON_TAY },
    { part: 'Iridium', xe: CON_TAY },

    // Ắc quy
    { part: 'Globe WP5S', xe: XE_GA },
    {
        part: 'GS 12V-5Ah', xe: [['Honda', 'Air Blade'], ['Honda', 'Vision'], ['Honda', 'SH'],
            ['Honda', 'SH Mode'], ['Honda', 'Vario'], ['Yamaha', 'Janus']],
    },
    {
        part: 'GT7A-H', xe: [['Honda', 'Lead'], ['Honda', 'SH'], ['Piaggio', 'Liberty'],
            ['SYM', 'Attila'], ['SYM', 'Elegant'], ['Yamaha', 'NVX']],
    },
    { part: 'GT6A', xe: XE_SO },

    // Lốp
    { part: 'Casumina', xe: XE_GA },
    { part: 'Maxxis', xe: XE_GA },
    { part: 'City Grip', xe: XE_GA },
    { part: 'IRC NR77', xe: XE_SO },
    { part: 'Pilot Street', xe: [...XE_SO, ...CON_TAY] },

    // Má phanh
    { part: 'Má phanh đĩa phổ thông', xe: MOI_XE },
    { part: 'Má phanh trước Honda Vision', xe: [['Honda', 'Vision']] },
    {
        part: 'Nissin', xe: [['Honda', 'Air Blade'], ['Honda', 'Lead'], ['Honda', 'SH'],
            ['Honda', 'SH Mode'], ['Honda', 'Vario'], ['Honda', 'Vision'],
            ['Honda', 'Blade'], ['Honda', 'Future'], ['Honda', 'Wave RSX']],
    },
    { part: 'Brembo', xe: CON_TAY },

    // Nhông sên dĩa
    { part: 'nhông sên dĩa phổ thông', xe: XE_SO },
    { part: 'DID cho Wave', xe: [['Honda', 'Wave Alpha'], ['Honda', 'Wave RSX']] },
    { part: 'Recto', xe: [...XE_SO, ...CON_TAY] },

    // Nước làm mát
    { part: 'GoRacing', xe: KET_NUOC },
    { part: 'Motocool', xe: KET_NUOC },

    // Dầu phanh
    { part: 'DOT 3', xe: MOI_XE },
    { part: 'Motul DOT 4', xe: MOI_XE },
    { part: 'RBF 600', xe: CON_TAY },

    // Lọc gió
    { part: 'Lọc gió xe số', xe: XE_SO },
    { part: 'Lọc gió Honda Air Blade', xe: [['Honda', 'Air Blade']] },
    { part: 'Lọc gió Honda Vision', xe: [['Honda', 'Vision'], ['Honda', 'Lead']] },
]

// ---------------------------------------------------------------- tiện ích

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

// ---------------------------------------------------------------- chạy

async function main() {
    if (!EMAIL || !PASSWORD) {
        console.error('Thiếu ADMIN_EMAIL hoặc ADMIN_PASSWORD. Xem hướng dẫn ở đầu file.')
        process.exit(1)
    }

    console.log(`Máy chủ: ${API}`)
    if (DRY_RUN) console.log('CHẾ ĐỘ THỬ: không ghi gì vào cơ sở dữ liệu.\n')

    // Lần gọi đầu có thể mất vài phút nếu máy chủ Render đang ngủ.
    console.log('Đang đăng nhập (máy chủ ngủ thì bước này mất vài phút)...')
    const auth = await call('/auth/login', { method: 'POST', body: { email: EMAIL, password: PASSWORD } })
    const token = auth.token
    console.log(`Đăng nhập xong: ${auth.user?.email ?? EMAIL}\n`)

    // Backend ép size tối đa 50 (ProductController), nên đọc từng trang cho tới hết.
    const parts = []
    for (let pageNo = 0; ; pageNo++) {
        const page = await call(`/products?type=sparepart&size=50&page=${pageNo}`, { token })
        parts.push(...(page.items ?? []))
        if (pageNo + 1 >= (page.totalPages ?? 1)) break
    }
    console.log(`Tìm thấy ${parts.length} phụ tùng trong kho.\n`)

    let created = 0
    let skipped = 0
    let failed = 0
    const missing = []

    for (const rule of RULES) {
        const needle = rule.part.toLowerCase()
        const found = parts.filter((p) => p.name.toLowerCase().includes(needle))

        if (found.length === 0) {
            missing.push(rule.part)
            console.log(`KHÔNG TÌM THẤY  "${rule.part}" — bỏ qua ${rule.xe.length} luật`)
            continue
        }
        if (found.length > 1) {
            console.log(`MƠ HỒ  "${rule.part}" khớp ${found.length} sản phẩm:`)
            found.forEach((p) => console.log(`        - ${p.name}`))
            console.log('        Sửa lại mẩu tên cho chính xác rồi chạy lại.')
            continue
        }

        const part = found[0]
        console.log(`${part.name}  (${rule.xe.length} dòng xe)`)

        for (const [brand, model] of rule.xe) {
            if (DRY_RUN) {
                console.log(`   thử: ${brand} ${model}`)
                continue
            }
            try {
                const r = await call('/admin/compatibility-rules', {
                    method: 'POST',
                    token,
                    body: {
                        sparePartId: part.productId,
                        brand,
                        model,
                        yearFrom: YEAR_FROM,
                        yearTo: YEAR_TO,
                    },
                })
                created += r.created ?? 0
                skipped += r.skipped ?? 0
                const note = r.created ? `+${r.created}` : 'đã có'
                console.log(`   ${brand} ${model}: ${note}${r.skipped ? ` (bỏ qua ${r.skipped})` : ''}`)
            } catch (e) {
                failed++
                console.log(`   ${brand} ${model}: LỖI — ${e.message}`)
            }
            await sleep(120)   // đừng dội quá nhanh vào máy chủ gói miễn phí
        }
        console.log('')
    }

    console.log('─'.repeat(50))
    console.log(`Đã tạo:   ${created} luật`)
    console.log(`Bỏ qua:   ${skipped} (đã có từ trước)`)
    console.log(`Lỗi:      ${failed}`)
    if (missing.length) {
        console.log(`\nChưa có trong kho, cần thêm sản phẩm trước rồi chạy lại:`)
        missing.forEach((m) => console.log(`  - ${m}`))
    }
}

main().catch((e) => {
    console.error('\nDừng vì lỗi:', e.message)
    process.exit(1)
})
