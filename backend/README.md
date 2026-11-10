# motoparts-backend — Project Skeleton

Backend dùng chung cho **Tiểu luận chuyên ngành** (website mua bán xe máy cũ & phụ tùng)
và **Khóa luận tốt nghiệp** (trợ lý AI tư vấn tương thích xe – phụ tùng).

Stack: Java 17 + Spring Boot 3.3 + Spring Data JPA + PostgreSQL (hoặc MySQL, xem bên dưới).

> Tài liệu nguồn cho toàn bộ thiết kế: project doc **"class-diagram-final-spec.md"**.
> Mọi entity/quan hệ ở bước "thiết kế schema" tiếp theo phải bám sát tài liệu đó.

## Vì sao package-by-feature (không phải package-by-layer)

Thay vì gom tất cả entity vào 1 package `entity`, tất cả repository vào 1 package `repository`,
dự án chia theo **nhóm nghiệp vụ** — đúng với cách class diagram đã nhóm các class (xem mục 2 của
tài liệu đặc tả). Mỗi nhóm nghiệp vụ tự có đủ 4 tầng bên trong: `entity / repository / service / controller`.
Cách này giúp:
- Tìm code liên quan đến 1 usecase nhanh hơn (vd: mọi thứ về giỏ hàng nằm trong `cart/`).
- Khi khóa luận AI phát triển thêm, chỉ cần thêm vào `advisory/` và `compatibility/`
  mà không đụng tới các phần khác.

## Cấu trúc package

```
com.nguyen.motoparts
├── account/            # User, Address
├── catalog/            # Product (abstract), Motorbike, SparePart
├── vehicle/             # VehicleModel, VehicleConditionAssessment, VehiclePhoto
├── compatibility/       # CompatibilityRule, ProductEmbedding
├── advisory/            # CustomerVehicleProfile, ChatSession, ChatMessage, AIRecommendationEngine (service only)
├── cart/                # ShoppingCart, CartItem
├── order/               # Order, OrderItem, InstallationRequest, Payment
├── aftersales/          # WarrantyRequest, ProductReview, Notification
└── common/
    ├── enums/           # Toàn bộ enum theo mục 3 đặc tả (ProductStatus, OrderStatus, ...)
    ├── exception/       # ResourceNotFoundException + GlobalExceptionHandler (@RestControllerAdvice)
    └── config/          # CorsConfig, ... (thêm SecurityConfig khi làm auth)
```

Mỗi package nghiệp vụ (trừ `common`) có dạng:
```
<feature>/
├── entity/          # @Entity stub — CHƯA có field, sẽ điền ở bước thiết kế schema
├── repository/       # interface extends JpaRepository<Entity, String>
├── service/          # @Service rỗng, chờ logic nghiệp vụ
└── controller/        # @RestController rỗng với @RequestMapping("/api/...") đã đặt sẵn route
```

`AIRecommendationEngine` chỉ có `service/` (đúng với đặc tả: đây là «service», không phải entity lưu trữ).

## Enum đã tạo sẵn (common/enums)

`ProductStatus`, `OrderStatus`, `PaymentMethod`, `PaymentStatus`, `InstallationStatus`,
`WarrantyStatus`, `NotificationType`, `RatingLevel`, `PartOriginType`, `SenderType`,
`AnswerStatus`, `UserRole` (phụ trợ cho `User.userRole`).

Giá trị trong mỗi enum hiện là **phỏng đoán hợp lý ban đầu** — sẽ rà lại khi thiết kế schema
(vd: `OrderStatus` có thể cần thêm trạng thái `AWAITING_PAYMENT` tùy quy trình thực tế).

## Database

Mặc định cấu hình PostgreSQL (`application.yml`). Nếu dùng MySQL:
1. Trong `pom.xml`: comment driver `postgresql`, bỏ comment driver `mysql-connector-j`.
2. Chạy với `--spring.profiles.active=mysql` (dùng cấu hình trong `application-mysql.yml`),
   hoặc sửa thẳng `application.yml`.

`ddl-auto: update` chỉ dùng tạm thời trong lúc phát triển — **chưa nên dùng ở bản nộp cuối**,
nên chuyển sang script SQL/migration (Flyway) khi schema đã chốt.

## Chạy thử

```bash
mvn spring-boot:run
```

(Yêu cầu PostgreSQL đang chạy local với database `motoparts_db`, user/pass `postgres/postgres`
— đổi trong `application.yml` nếu khác.)

## Bước tiếp theo

1. **Thiết kế schema**: điền field + quan hệ JPA (`@ManyToOne`, `@OneToMany`, `@Embedded`, ...)
   vào từng entity stub, đúng theo mục 2 của tài liệu đặc tả (đặc biệt chú ý các quyết định
   đã chốt: `Motorbike` không liên kết `VehicleModel`, `CustomerVehicleProfile`/`WarrantyRequest`/
   `ProductReview`/`Notification` chỉ giữ `accountId` dạng String, không vẽ Association tới `User`).
2. Viết DTO + mapping (entity ⇄ DTO) cho từng controller.
3. Viết logic nghiệp vụ trong các `*Service` theo use case tương ứng.
4. `AIRecommendationEngine`: tích hợp sau, khi có dữ liệu `CompatibilityRule` / `ProductEmbedding` thật.

## Đăng nhập, JWT và biến môi trường (Người B)

API đăng nhập (không cần token): `POST /api/auth/register`, `POST /api/auth/login` (trả `token`).
Có token: gửi header `Authorization: Bearer <token>`; `GET /api/auth/me` cho biết ai đang đăng nhập.
`GET /api/products|motorbikes|spare-parts|vehicle-models/**` mở cho khách chưa đăng nhập; `/api/admin/**` chỉ ADMIN;
các đường còn lại cần đăng nhập. Phân quyền chi tiết hơn dùng `@PreAuthorize("hasRole('ADMIN')")` ở controller.
Tài khoản bị khóa (có dòng trong `AccountLock`) bị chặn ở mọi request, kể cả khi token còn hạn.

| Biến môi trường | Ý nghĩa |
|---|---|
| `JWT_SECRET` | Khóa ký JWT, ít nhất 32 ký tự. Bỏ trống: dùng khóa ngẫu nhiên mỗi lần chạy (chỉ để dev). Bắt buộc đặt khi deploy. |
| `JWT_EXPIRATION_MINUTES` | Thời hạn token, mặc định 120. |
| `CORS_ALLOWED_ORIGINS` | Địa chỉ React được gọi API, cách nhau bằng dấu phẩy. Mặc định `http://localhost:5173`; khi deploy là tên miền Static Site. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Tùy chọn: tạo admin đầu tiên khi khởi động (mật khẩu >= 8 ký tự). Đã có email đó thì không ghi đè. |

Kiểm thử bảo mật cần PostgreSQL: đặt `TEST_DB_URL` (vd `jdbc:postgresql://localhost:5432/motoparts_test`),
`TEST_DB_USERNAME`, `TEST_DB_PASSWORD` rồi chạy `mvn test -Dtest=AuthSecurityTest`. Không đặt `TEST_DB_URL` thì test tự bỏ qua.

## Tài khoản (tuần 2): quên mật khẩu, admin khóa tài khoản, sổ địa chỉ

Không cần đăng nhập:

| Method | Đường dẫn | Việc |
|---|---|---|
| POST | `/api/auth/forgot-password` `{email}` | Gửi liên kết đặt lại mật khẩu. Luôn trả cùng một thông báo, dù email có tài khoản hay không |
| POST | `/api/auth/reset-password/validate` `{token}` | `{valid: true/false}`: React gọi khi mở trang để báo liên kết hết hạn sớm |
| POST | `/api/auth/reset-password` `{token, password, confirmPassword}` | Đặt mật khẩu mới (8 đến 72 ký tự). Mã chỉ dùng một lần |

Liên kết trong email có dạng `FRONTEND_URL/reset-password?token=...`, hết hạn sau 30 phút, mỗi tài khoản tối đa 5 yêu cầu mỗi giờ.
Chỉ lưu SHA-256 của mã trong CSDL. Yêu cầu mới thay thế mã cũ chưa dùng.

Chỉ ADMIN (`/api/admin/accounts`):

| Method | Đường dẫn | Việc |
|---|---|---|
| GET | `/api/admin/accounts?q=&status=ALL\|ACTIVE\|LOCKED&page=&size=` | Danh sách khách hàng (không gồm admin), kèm trạng thái khóa và số đơn chưa hoàn tất |
| POST | `/api/admin/accounts/{id}/lock` `{reason, force}` | Khóa. Còn đơn chưa hoàn tất thì trả 409 và cần gửi lại với `force: true` |
| POST | `/api/admin/accounts/{id}/unlock` | Mở khóa |

Cần đăng nhập (của chính người dùng, `accountId` lấy từ token), tối đa 10 địa chỉ mỗi tài khoản:

| Method | Đường dẫn | Việc |
|---|---|---|
| GET | `/api/addresses` | Sổ địa chỉ, địa chỉ mặc định lên đầu |
| POST | `/api/addresses` | Thêm. Địa chỉ đầu tiên tự là mặc định |
| PUT | `/api/addresses/{id}` | Sửa (`makeDefault: true` để đặt mặc định) |
| POST | `/api/addresses/{id}/default` | Đặt làm mặc định |
| DELETE | `/api/addresses/{id}` | Xóa. Xóa địa chỉ mặc định thì địa chỉ cũ nhất còn lại thành mặc định |

Nội dung địa chỉ: `receiverName, receiverPhone, houseNumber, street, district, city, makeDefault`.

Gửi email (biến môi trường mới):

| Biến | Ý nghĩa |
|---|---|
| `FRONTEND_URL` | Gốc địa chỉ React để tạo liên kết trong email (mặc định `http://localhost:5173`) |
| `BREVO_API_KEY`, `MAIL_FROM`, `MAIL_FROM_NAME` | Gửi qua Brevo API (HTTPS). Dùng cách này trên Render vì Render chặn cổng SMTP. `MAIL_FROM` phải là địa chỉ đã xác minh trong Brevo |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_STARTTLS` | Gửi qua SMTP (chạy máy cá nhân). Có cả Brevo và SMTP thì dùng Brevo |
| `RESET_EXPIRE_MINUTES`, `RESET_MAX_PER_HOUR` | Thời hạn liên kết (30) và số yêu cầu tối đa mỗi giờ (5) |

Không cấu hình email nào: chế độ phát triển, nội dung email (kèm liên kết đặt lại mật khẩu) chỉ in ra log, không gửi đi.
Kiểm thử: `mvn test -Dtest=AccountFeaturesTest` (cần `TEST_DB_URL`, xem mục trên).
