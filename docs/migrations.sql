-- Ghi lại mọi lệnh ALTER/UPDATE đã chạy trên CSDL Supabase dùng chung, theo thứ tự thời gian.
-- Mỗi lệnh kèm một dòng chú thích: ngày, người chạy, mục đích.
-- 2026-10-06: schema ban đầu do JPA tạo tự động (schema-generation = create), chưa có lệnh nào cần ghi.
-- 2026-10-07: feature/cart them hai bang moi shoppingcart va cartitem (JPA tu tao khi chay lan dau, khong co ALTER nao tren bang cu).
-- 2026-10-07: feature/order them 3 bang moi orders, orderitem, payment (JPA tu tao khi chay lan dau, khong co ALTER nao tren bang cu). Dat hang chuyen xe sang status SOLD trong bang product.
-- 2026-10-07: feature/admin-orders khong them bang hay cot moi; Admin doi trang thai don hang, xac nhan thu tien COD (payment.status -> PAID), huy don tra xe ve AVAILABLE trong bang product.
-- 2026-10-08: feature/product-review them bang moi productreview (JPA tu tao khi chay lan dau, khong co ALTER nao tren bang cu).
-- 2026-10-08: feature/warranty them bang moi warrantyrequest (JPA tu tao khi chay lan dau, khong co ALTER nao tren bang cu).
-- 2026-10-08: feature/installation them bang moi installationrequest (JPA tu tao khi chay lan dau). DeliveryMethod them gia tri INSTALL (cot deliverymethod luu chuoi nen khong can ALTER). Phi cong lap dat duoc cong vao orders.totalamount.
-- 2026-10-08: feature/account-management them bang moi accountlock (JPA tu tao khi chay lan dau, khong co ALTER nao tren bang cu; khong them cot vao bang users). Co dong trong accountlock = tai khoan bi khoa.
-- 2026-10-08: feature/reset-password them bang moi passwordresettoken (JPA tu tao khi chay lan dau, khong co ALTER nao tren bang cu; khong doi bang users). Them thu vien angus-mail vao pom.xml va cau hinh email mail.properties (khong commit, xem mail.example.properties).
-- 2026-10-08: feature/address them bang moi address (JPA tu tao khi chay lan dau, khong co ALTER nao tren bang cu). Don hang van chup lai ho ten, so dien thoai, dia chi nen khong tham chieu Address.
-- 2026-10-08: feature/vnpay them bang moi onlinepayment (JPA tu tao khi chay lan dau, khong co ALTER nao tren bang cu). Moi lan tra tien VNPay la mot dong; Payment van la nguon su that ve da thanh toan hay chua. Can file src/main/resources/vnpay.properties tren moi may (xem vnpay.example.properties), khong commit.
-- 2026-10-08: feature/vehicle-photo them bang moi vehiclephoto, tao thu cong tren Supabase (khong de JPA tu tao) de co khoa ngoai ON DELETE CASCADE toi motorbike(productid) va bat Row Level Security. Anh luu tren Supabase Storage (bucket vehicle-photos), cot imageurl la link cong khai. Can 3 bien moi truong tren moi may: SUPABASE_URL, SUPABASE_BUCKET, SUPABASE_KEY (khong commit).
CREATE TABLE IF NOT EXISTS vehiclephoto (
                                            photoid      VARCHAR(255) PRIMARY KEY,
    imageurl     VARCHAR(500),
    caption      VARCHAR(255),
    storagepath  VARCHAR(255),
    motorbike_id VARCHAR(255) NOT NULL REFERENCES motorbike(productid) ON DELETE CASCADE
    );
ALTER TABLE vehiclephoto ENABLE ROW LEVEL SECURITY;

-- 2026-10-08: feature/sparepart-photo them bang moi sparepartphoto, tao thu cong tren Supabase (khong de JPA tu tao) de co khoa ngoai ON DELETE CASCADE toi sparepart(productid) va bat Row Level Security. Dung chung bucket vehicle-photos (thu muc spareparts/), cot imageurl la link cong khai.
CREATE TABLE IF NOT EXISTS sparepartphoto (
                                              photoid      VARCHAR(255) PRIMARY KEY,
    imageurl     VARCHAR(500),
    caption      VARCHAR(255),
    storagepath  VARCHAR(255),
    sparepart_id VARCHAR(255) NOT NULL REFERENCES sparepart(productid) ON DELETE CASCADE
    );
ALTER TABLE sparepartphoto ENABLE ROW LEVEL SECURITY;

