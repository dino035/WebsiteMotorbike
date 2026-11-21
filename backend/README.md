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

| `SUPABASE_URL` | Địa chỉ project Supabase dùng lưu ảnh sản phẩm. Bỏ trống thì mọi thao tác ảnh báo lỗi. |
| `SUPABASE_BUCKET` | Tên bucket chứa ảnh, mặc định `photos`. Bucket phải đặt Public thì trình duyệt mới hiển thị được ảnh. |
| `SUPABASE_KEY` | Khóa service_role của Supabase. Bí mật: chỉ đặt bằng biến môi trường, không ghi vào file hay Git, không bao giờ đưa sang React. |

Khi deploy lên Render phải đặt đủ ba biến trên, không thì toàn bộ tính năng ảnh hỏng.

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

Giao diện React: `/admin/accounts` (tab "Tài khoản" trong trang quản trị). Tìm theo email, lọc Tất cả / Đang hoạt động / Đã khóa, phân trang; bộ lọc nằm trên địa chỉ. Khóa có ô nhập lý do; tài khoản còn đơn chưa hoàn tất phải tick xác nhận mới khóa được (gửi `force: true`). Mở khóa có bước xác nhận.

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

## Giỏ hàng và tạm giữ xe

Cần đăng nhập (giỏ của chính người dùng, `accountId` lấy từ token). Mọi thao tác trả lại giỏ mới nhất:
`{lines, total, itemCount, notices, holdMinutes}`.

| Method | Đường dẫn | Việc |
|---|---|---|
| GET | `/api/cart` | Xem giỏ. Dòng không còn khả dụng được gỡ và ghi vào `notices` (hiện một lần) |
| POST | `/api/cart/items` `{productId, quantity}` | Thêm. Phụ tùng cộng dồn số lượng (tối đa 99); xe luôn là 1 và được giữ 30 phút, thêm lại thì gia hạn |
| PUT | `/api/cart/items/{cartItemId}` `{quantity}` | Sửa số lượng (1 đến 99). Xe máy chỉ mua được 1 chiếc (400) |
| DELETE | `/api/cart/items/{cartItemId}` | Xóa một dòng |

Mã lỗi: 404 không có sản phẩm hoặc dòng (kể cả dòng của người khác), 409 sản phẩm hết hàng hoặc xe đang được khách khác giữ, 400 số lượng sai.

Luật tạm giữ xe: không có bộ đếm giờ, chỉ so `addedAt` với bây giờ. Hết 30 phút mà chưa ai khác thêm xe thì xe vẫn của giỏ này.
Khi có người khác thêm và giữ, dòng của người hết hạn bị gỡ ở lần mở giỏ sau. Hai khách thêm cùng một xe cùng lúc thì chỉ một người giữ được
(khóa dòng sản phẩm `SELECT ... FOR UPDATE`; bản JSP cũ chưa chặn chỗ này). Đổi thời gian giữ bằng biến `CART_HOLD_MINUTES` (mặc định 30).
Kiểm thử: `mvn test -Dtest=CartTest`.

## Đặt hàng (COD, giao tận nơi, nhận tại cửa hàng, lắp đặt)

Cần đăng nhập. Đặt hàng từ giỏ hiện tại của người dùng; `paymentMethod` là `COD` hoặc `VNPAY` (VNPay xem mục riêng bên dưới; chưa cấu hình VNPay thì chọn `VNPAY` trả 400).

| Method | Đường dẫn | Việc |
|---|---|---|
| GET | `/api/orders/policy` | Phí công lắp đặt, giới hạn ngày hẹn và `vnpayEnabled`, để React vẽ trang đặt hàng |
| POST | `/api/orders` | Đặt hàng từ giỏ (201, trả chi tiết đơn) |
| GET | `/api/orders` | Đơn của tôi, mới nhất trước |
| GET | `/api/orders/{orderId}` | Chi tiết một đơn (đơn của người khác trả 404) |

Thân `POST /api/orders`: `{deliveryMethod: DELIVERY\|PICKUP\|INSTALL, receiverName, receiverPhone, shippingAddress, paymentMethod: COD\|VNPAY, returnPolicyAccepted: true, installDate}`.
`shippingAddress` bắt buộc khi `DELIVERY`; `installDate` (yyyy-MM-dd) bắt buộc khi `INSTALL`. Người nhận và địa chỉ được chụp vào đơn
(sửa hay xóa sổ địa chỉ sau đó không đổi đơn cũ); tên và đơn giá sản phẩm cũng chụp lúc đặt.

Đặt hàng chạy trong một giao dịch: khóa tài khoản, khóa từng sản phẩm (theo thứ tự mã), kiểm tra lại còn bán được, tạo đơn
`PENDING` + dòng hàng + thanh toán COD `PENDING`, chuyển xe sang `SOLD`, xóa giỏ, ghi thông báo `ORDER_PLACED`.

- Lắp đặt (`INSTALL`): chỉ đơn có phụ tùng; ngày hẹn từ ngày mai tới 60 ngày; tối đa 5 lượt mỗi ngày (khóa theo ngày để hai khách cùng đặt
  không vượt); phí công 50.000 đ cộng vào tổng đơn. Các con số là giả định từ bản JSP, cần chốt với nhóm (`InstallationPolicy`).
- Mã lỗi: 400 giỏ trống, thiếu hay sai dữ liệu (địa chỉ, số điện thoại, chưa đồng ý chính sách đổi trả), 409 sản phẩm không còn khả dụng hoặc ngày kín lịch.
  Đơn bị từ chối thì không tiêu thụ gì: giỏ và trạng thái xe còn nguyên.
- Hai lần đặt cùng lúc từ một giỏ chỉ tạo một đơn (khóa tài khoản).

Admin xem phần "Admin quản lý đơn hàng" bên dưới. Kiểm thử: `mvn test -Dtest=OrderTest`.

## Thanh toán VNPay Sandbox (SRS 3.10)

Khách chọn `VNPAY` khi đặt hàng, rồi được chuyển sang cổng VNPay (môi trường thử nghiệm) để trả tiền. Mỗi lần bấm trả tiền là một bản ghi `OnlinePayment` với mã giao dịch (`vnp_TxnRef`) mới; một đơn có thể thử lại nhiều lần.

| Method | Đường dẫn | Việc |
|---|---|---|
| POST | `/api/orders/{orderId}/vnpay` | Cần đăng nhập, đúng chủ đơn. Tạo một lần trả tiền và trả `{paymentUrl, txnRef}`; React chuyển trình duyệt sang `paymentUrl` |
| POST | `/api/orders/{orderId}/pay-cod` | Đổi đơn VNPay chưa trả được tiền sang COD (SRS 3.10); trả chi tiết đơn |
| GET | `/api/payments/vnpay/return` | Không cần đăng nhập. Trang React `/payment/vnpay-return` gửi nguyên các tham số `vnp_*` sang đây; kiểm tra chữ ký, ghi nhận kết quả, trả `{outcome, paid, message, orderId, orderCode, reason}` |
| GET | `/api/payments/vnpay/ipn` | Không cần đăng nhập. VNPay gọi thẳng máy chủ; trả `{RspCode, Message}` theo quy định của VNPay (00 đã nhận, 01 không thấy giao dịch, 02 đã xử lý, 04 sai số tiền, 97 sai chữ ký, 99 chưa cấu hình) |

**Biến môi trường (khóa tuyệt đối không ghi vào file hay Git):**

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `VNPAY_TMN_CODE` | (trống) | Terminal ID (mã website) của tài khoản sandbox |
| `VNPAY_HASH_SECRET` | (trống) | Chuỗi bí mật tạo chữ ký |
| `VNPAY_PAY_URL` | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` | Địa chỉ cổng thanh toán |
| `VNPAY_RETURN_URL` | `<FRONTEND_URL>/payment/vnpay-return` | Nơi VNPay đưa trình duyệt của khách quay về (trang React) |
| `VNPAY_EXPIRE_MINUTES` | 15 | Thời gian khách được trả tiền trên cổng (5 đến 60) |

Thiếu `VNPAY_TMN_CODE` hoặc `VNPAY_HASH_SECRET` thì VNPay bị khóa ở mọi cửa (`vnpayEnabled: false`, đặt hàng VNPAY trả 400, return trả 400, IPN trả 99), đơn vẫn đặt được bằng COD.

**Chữ ký:** tham số `vnp_*` sắp theo tên, bỏ tham số rỗng, mã hóa URL giá trị, nối bằng `&`, ký HMAC-SHA512 bằng `VNPAY_HASH_SECRET` (phiên bản 2.1.0). Thông báo chỉ được tin khi chữ ký khớp và `vnp_TmnCode` đúng mã website của mình; số tiền gửi là VND nhân 100, giờ gửi là GMT+7 dù máy chủ ở múi giờ nào.

**Ghi nhận kết quả (dùng chung cho return và IPN, chạy lặp lại không ghi trùng):** khóa dòng đơn (`SELECT ... FOR UPDATE`) rồi mới đọc giao dịch, nên return và IPN đến cùng lúc chỉ ghi nhận một lần. Các kết quả (`outcome`):
`PAID` (đơn chuyển đã thanh toán, ghi `paidAt`, thông báo cho khách), `FAILED` (khách hủy, hết hạn hay ngân hàng từ chối; đơn chuyển `FAILED`, vẫn thử lại hoặc đổi sang COD được),
`ALREADY_PROCESSED` (đã ghi nhận trước đó, ví dụ IPN đến trước khi khách quay về; `paid` vẫn phản ánh đúng kết quả lần trả đó),
`AMOUNT_MISMATCH` (số tiền VNPay báo khác số tiền giao dịch: không ghi nhận gì),
`NOT_FOUND`, và hai trường hợp đã thu tiền nhưng không áp dụng được cho đơn cần hoàn tiền thủ công: `PAID_ORDER_CANCELLED` (đơn đã bị hủy) và `PAID_DUPLICATE` (đơn đã được thanh toán bằng lần khác). Cả hai đều ghi thông báo cho khách, và Admin thấy giao dịch (mã giao dịch, mã VNPay, ngân hàng) trong chi tiết đơn để đối soát. Tiền đã thu thật thì đơn ở trạng thái đã thanh toán dù đơn bị hủy.
Thông báo thất bại muộn của một lần cũ không hạ đơn đã thanh toán; tiền nhận được sau khi khách đã đổi sang COD vẫn được ghi là đã trả bằng VNPay.

**Chạy trên máy (localhost):** VNPay không gọi IPN tới `localhost` được, nên kết quả được ghi nhận khi trình duyệt quay về `/payment/vnpay-return`. IPN chỉ cần khi triển khai có địa chỉ công khai (khai địa chỉ IPN trong trang Merchant của VNPay). Thẻ thử của sandbox: ngân hàng NCB, số thẻ `9704198526191432198`, tên `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`.

**Chưa làm:** hết hạn tự động cho đơn VNPay chưa trả (xe vẫn ở `SOLD` cho tới khi Admin hủy đơn), tra cứu và hoàn tiền qua API của VNPay (hoàn tiền làm thủ công trên trang Merchant).

**Kiểm thử:** `order/service/VnpayGatewayTest` (không cần CSDL: chữ ký so với cài đặt HMAC độc lập viết bằng Python, dữ liệu ký, liên kết thanh toán, từ chối chữ ký sai hoặc mã website khác, giờ Việt Nam) và `order/VnpayPaymentTest` (PostgreSQL thật, test tự ký thông báo như VNPay: đặt hàng, tạo liên kết, ghi nhận thành công một lần, IPN trước khi quay về, chữ ký sai hay số tiền sai, thử lại sau khi thất bại, đổi sang COD, đơn đã hủy, trả trùng, 8 luồng return và IPN cùng lúc).

## Admin quản lý đơn hàng và lịch lắp đặt

Chỉ ADMIN (`/api/admin/**`).

| Method | Đường dẫn | Việc |
|---|---|---|
| GET | `/api/admin/orders?status=&page=&size=` | Mọi đơn, mới nhất trước; lọc theo trạng thái (bỏ trống hoặc `ALL` là không lọc); trả kèm email khách |
| GET | `/api/admin/orders/{orderId}` | Chi tiết: đơn, khách, `nextStatuses` (các bước được chuyển), `canConfirmPayment`, `onlinePayments` (các lần trả tiền VNPay để đối soát) |
| POST | `/api/admin/orders/{orderId}/status` `{status}` | Đổi trạng thái; chỉ theo `OrderStatus.canMoveTo` |
| POST | `/api/admin/orders/{orderId}/confirm-payment` | Xác nhận đã thu tiền COD |
| GET | `/api/admin/installations?status=` | Lịch lắp đặt, ngày hẹn gần nhất trước |
| POST | `/api/admin/installations/{requestId}/action` `{action: DONE\|CANCEL\|RESCHEDULE, newDate}` | Xử lý một lịch đang `SCHEDULED` |

Luật: hoàn thành đơn (`COMPLETED`) cần đã thu tiền; hủy đơn (`CANCELLED`) cần chưa thu tiền, trả các xe trong đơn về `AVAILABLE`
và hủy luôn lịch lắp đặt (trả chỗ trống). Đánh dấu lịch lắp xong cần đơn đã xác nhận; đổi ngày kiểm tra ngày còn chỗ
(tối đa 5 lượt mỗi ngày, khóa theo ngày). Mọi thay đổi đều ghi thông báo cho khách.
Mã lỗi: 404 không có đơn hoặc lịch, 409 bước chuyển không hợp lệ, chưa/đã thu tiền, ngày kín lịch, lịch đã xử lý; 400 giá trị trạng thái hay ngày sai.
Hai Admin đổi cùng một đơn cùng lúc: khóa dòng đơn (`SELECT ... FOR UPDATE`) nên chỉ một thao tác thắng (bản JSP cũng khóa; dùng truy vấn JPQL thay vì `em.find`
vì `em.find` nối bảng lắp đặt, PostgreSQL không cho `FOR UPDATE` ở phía có thể rỗng của LEFT JOIN).
Phí công vẫn nằm trong tổng đơn khi lịch lắp đặt bị hủy riêng (giữ như bản JSP, cần chốt với nhóm).
Kiểm thử: `mvn test -Dtest=AdminOrderTest`.

## Bảo hành (khách gửi yêu cầu, Admin duyệt hoặc từ chối)

Cần đăng nhập; phần Admin chỉ ADMIN.

| Method | Đường dẫn | Việc |
|---|---|---|
| GET | `/api/warranty-requests/options` | Sản phẩm đã mua trong đơn Hoàn thành, kèm `purchasedAt`, `deadline`, `expired`, `pending`, `canRequest` |
| POST | `/api/warranty-requests` `{orderItemId, description, attachmentUrl}` | Gửi yêu cầu (201). `attachmentUrl` (ảnh/video minh chứng) không bắt buộc |
| GET | `/api/warranty-requests` | Yêu cầu của tôi, mới nhất trước |
| GET | `/api/warranty-requests/{requestId}` | Chi tiết một yêu cầu (của người khác trả 404) |
| GET | `/api/admin/warranty-requests?status=&page=&size=` | Mọi yêu cầu, mới nhất trước, lọc theo `PENDING`/`APPROVED`/`REJECTED` |
| GET | `/api/admin/warranty-requests/{requestId}` | Chi tiết: yêu cầu, khách, ngày mua, hạn bảo hành, `canDecide` |
| POST | `/api/admin/warranty-requests/{requestId}/decide` `{approve, note}` | Duyệt (thay mới) hoặc từ chối; từ chối bắt buộc có `note` là lý do |

Gửi yêu cầu cần: sản phẩm thuộc khách (không thì 404), đơn đã Hoàn thành, còn trong hạn (xe 24 tháng, phụ tùng 6 tháng, tính từ ngày đặt đơn;
sản phẩm đã bị xóa khỏi hệ thống tính như phụ tùng), chưa có yêu cầu đang chờ xử lý cho sản phẩm đó (409 ở các trường hợp còn lại). Bị từ chối rồi thì gửi lại được.
`attachmentUrl` chỉ nhận `http://` hoặc `https://` (Admin sẽ bấm vào đường dẫn, nên chặn `javascript:` và các kiểu khác). Gửi xong có thông báo cho mọi Admin;
quyết định xong có thông báo cho khách. Bấm gửi hai lần cùng lúc chỉ tạo một yêu cầu (khóa tài khoản); hai Admin quyết định cùng một yêu cầu cùng lúc chỉ một người thắng
(khóa dòng yêu cầu `SELECT ... FOR UPDATE`; bản JSP chỉ chặn ở phía Admin). Hạn 24 tháng và 6 tháng lấy từ SRS 2.3.2 (`WarrantyPolicy`).
Kiểm thử: `mvn test -Dtest=WarrantyTest`.


## Thông báo của khách

Thông báo do hệ thống tạo (đặt hàng, đổi trạng thái đơn, lịch lắp đặt, bảo hành được duyệt hoặc từ chối). Tài khoản nào chỉ thấy và sửa được thông báo của chính mình (`accountId` lấy từ token). Cần đăng nhập.

| Phương thức | Đường dẫn | Việc làm |
|---|---|---|
| GET | `/api/notifications?page=0&size=20` | Danh sách, mới nhất trước, phân trang (`size` tối đa 50). Mỗi dòng: `notificationId`, `type`, `typeLabel`, `content`, `isRead`, `createdAt` |
| GET | `/api/notifications/unread-count` | `{ "count": n }` cho chuông trên menu |
| POST | `/api/notifications/{id}/read` | Đánh dấu đã đọc một thông báo; gọi lại vẫn được; của người khác hoặc không có thì 404 |
| POST | `/api/notifications/read-all` | Đánh dấu tất cả của mình đã đọc, trả `{ "updated": n }` |

Đánh dấu đã đọc dùng một câu `UPDATE ... WHERE id = ? AND accountId = ?` nên điều kiện chủ sở hữu nằm ngay trong câu lệnh, không có khoảng hở giữa kiểm tra và ghi.

React: mục "Thông báo (n)" trên menu (số chưa đọc tải lại khi đổi trang và mỗi 60 giây), trang `/notifications` có phân trang, bấm một thông báo để đánh dấu đã đọc, nút "Đánh dấu tất cả đã đọc".

Kiểm thử: `aftersales/NotificationTest` (8 ca): phải đăng nhập, khách mới chưa có gì, mới nhất trước, mỗi người chỉ thấy của mình, phân trang ổn định, đánh dấu đã đọc lặp lại không lỗi, không đánh dấu được của người khác, đánh dấu tất cả không đụng tài khoản khác.


## Chat box AI (gợi ý sản phẩm)

Khách hỏi bằng tiếng Việt ("nhớt cho Honda Wave 2015 dưới 200k", "xe số đời 2018 trở lên dưới 20 triệu"), chat box gợi ý tối đa 5 sản phẩm thật của cửa hàng. Cần đăng nhập.

**Cách hoạt động (LLM không tự chọn sản phẩm):**
1. Gemini đọc câu hỏi (kèm tối đa 6 lượt hội thoại trước) thành tiêu chí JSON: loại (xe máy / phụ tùng), hãng, dòng, năm xe, khoảng giá, loại phụ tùng, nguồn gốc...
2. Code lọc sản phẩm trong CSDL (`ChatProductSearch`): chỉ hàng còn bán, rẻ nhất trước. Phụ tùng theo xe của khách đi qua luật tương thích (`CompatibilityRule`): dòng xe khớp hãng + tên và khoảng năm sản xuất bao trùm năm đăng ký.
3. Gemini viết lời tư vấn chỉ dựa trên các sản phẩm đã lọc. Lời gọi Gemini nằm sau interface `LlmClient` nên đổi nhà cung cấp không phải sửa bộ máy gợi ý.

Chỉ câu hỏi, lịch sử hội thoại và dữ kiện sản phẩm (tên, loại, giá, mô tả) được gửi cho Gemini; không gửi email, số điện thoại hay địa chỉ của khách. Khóa gửi trong header `x-goog-api-key`, không nằm trên URL.

**Câu hỏi nào là "chưa trả lời được" (UNANSWERED):** ngoài phạm vi xe và phụ tùng, không có sản phẩm còn hàng khớp, dòng xe chưa có dữ liệu tương thích, hoặc Gemini không dùng được lúc đó. Admin lọc các câu này để biết khách đang cần gì mà cửa hàng chưa có. Chào hỏi được trả lời bình thường. Gemini lỗi ở bước viết lời đáp nhưng đã có sản phẩm thật thì dùng lời đáp dựng sẵn, câu hỏi vẫn tính là đã trả lời.

| Phương thức | Đường dẫn | Việc làm |
|---|---|---|
| POST | `/api/chat-sessions` | Tạo cuộc trò chuyện mới |
| GET | `/api/chat-sessions` | 20 cuộc gần nhất của tôi (tiêu đề là câu hỏi đầu) |
| GET | `/api/chat-sessions/{id}` | Toàn bộ tin nhắn, kèm sản phẩm đã gợi ý trong từng câu trả lời |
| POST | `/api/chat-sessions/{id}/messages` | Gửi câu hỏi (`content`, tối đa 500 ký tự), trả `customerMessage` (có `status`) và `assistantMessage` (có `products`) |
| GET | `/api/admin/chat/questions?status=UNANSWERED&page=&size=` | Admin: các câu hỏi của khách, lọc theo trạng thái |
| GET | `/api/admin/chat/sessions/{id}` | Admin: đọc cả cuộc trò chuyện, kèm email khách |

Cuộc trò chuyện của người khác trả 404. Mỗi tài khoản tối đa 30 câu hỏi mỗi giờ (`CHAT_MAX_MESSAGES_PER_HOUR`), vượt thì 429, để giữ hạn mức gói miễn phí của Gemini. Mỗi cuộc tối đa 100 tin nhắn. Giao dịch CSDL không bị giữ trong lúc chờ Gemini.

**Biến môi trường:**

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `GEMINI_API_KEY` | (trống) | Khóa tạo ở Google AI Studio. Chưa đặt thì chat vẫn chạy, mọi câu hỏi là "chưa trả lời được". Chỉ đặt bằng biến môi trường, không ghi vào file hay Git |
| `GEMINI_MODEL` | `gemini-flash-latest` | Tên model; xem tên và hạn mức hiện có trong AI Studio. Ngày 20/11/2026 với khóa AI Studio thử: `gemini-flash-latest` treo quá thời gian chờ, `gemini-2.5-flash` trả 404, `gemini-3.5-flash` trả 503; `gemini-flash-lite-latest` chạy ổn (khoảng 3 giây mỗi câu). Danh sách này thay đổi theo thời điểm và khóa nên cần thử lại khi lỗi |
| `GEMINI_TIMEOUT_SECONDS` | 20 | Thời gian chờ mỗi lời gọi |
| `CHAT_MAX_MESSAGES_PER_HOUR` | 30 | Giới hạn câu hỏi mỗi tài khoản mỗi giờ |

Bảng `chatmessage` có thêm cột `product_ids` (mã các sản phẩm đã gợi ý, cách nhau dấu phẩy); Hibernate tự thêm khi chạy.

**Kiểm thử:** `advisory/GeminiLlmClientTest` (9 ca, máy chủ HTTP giả, không cần CSDL): khóa nằm trong header, đọc JSON tiêu chí khoan dung với giá trị sai, mọi kiểu lỗi thành `LlmUnavailableException`, không có khóa thì không gọi ra ngoài. `advisory/ChatTest` (21 ca, PostgreSQL thật, LLM giả): chỉ hàng còn bán và rẻ nhất trước, tối đa 5, ký tự `%` từ LLM không thành mẫu tìm kiếm, phụ tùng chỉ theo luật tương thích và khoảng năm, các tình huống UNANSWERED, Gemini lỗi vẫn lưu hội thoại, giới hạn tần suất, quyền Admin. Chưa gọi Gemini thật (cần khóa).

**Giao diện React:** `/chat` (cần đăng nhập, menu "Hỏi AI"): danh sách cuộc trò chuyện bên trái, khung hội thoại bên phải, câu trả lời kèm thẻ sản phẩm bấm vào xem chi tiết. Cuộc mới chỉ được tạo khi khách gửi câu đầu tiên. Cuộc đang mở nằm trên địa chỉ (`?s=`) nên tải lại vẫn giữ. Admin: `/admin/chat` lọc câu hỏi (mặc định "Chưa trả lời được") và `/admin/chat/:id` đọc cả cuộc trò chuyện.

**Đã chạy thử với Gemini thật** (20/11/2026, model `gemini-flash-lite-latest`): chào hỏi, tìm xe máy theo hãng và giá, hỏi phụ tùng khi chưa biết xe (hỏi lại khách), câu hỏi ngoài phạm vi (từ chối lịch sự, ghi nhận cho Admin) đều đúng. Gemini có thể tự thêm chi tiết không có trong dữ liệu sản phẩm (ví dụ số km, "chính chủ") dù lời nhắc cấm; chưa siết thêm.

## Hồ sơ xe của khách (SRS 3.15)

Khách lưu các xe mình đang có để chat gợi ý phụ tùng lắp vừa xe đó mà không phải nói lại xe mỗi lần. Cần đăng nhập; mọi thao tác chỉ trên hồ sơ của chính khách (`accountId` lấy từ token), hồ sơ của người khác trả 404.

| Phương thức | Đường dẫn | Việc làm |
|---|---|---|
| GET | `/api/vehicle-profiles` | Các xe của tôi, mới nhất trước |
| POST | `/api/vehicle-profiles` | Thêm xe: `{nickname, vehicleModelId, registrationYear}` |
| PUT | `/api/vehicle-profiles/{id}` | Sửa (cùng dữ liệu và cùng kiểm tra như thêm) |
| DELETE | `/api/vehicle-profiles/{id}` | Xóa; các cuộc trò chuyện đang gắn với xe này chuyển thành hỏi chung |

Kiểm tra: dòng xe phải có trong bảng `VehicleModel`; năm đăng ký phải nằm trong khoảng năm sản xuất của dòng xe đó (sai thì 400, báo rõ khoảng năm); biệt danh bắt buộc và tối đa 50 ký tự (trống hay toàn khoảng trắng trả 400); tối đa 5 hồ sơ xe mỗi khách theo QĐ03 và test case SB07 trong SRS (thêm xe thứ 6 trả 409; thêm đồng thời vẫn không vượt giới hạn vì khóa dòng tài khoản).

**Dùng trong chat:** `POST /api/chat-sessions` nhận thêm `{"profileId": "..."}` (không bắt buộc; hồ sơ của người khác hoặc không tồn tại trả 404). Phản hồi cuộc trò chuyện có thêm `vehicle` (hồ sơ đang tư vấn cho, hoặc `null`). Khi khách hỏi **phụ tùng** mà câu hỏi không nhắc hãng, dòng hay năm xe nào, bộ máy gợi ý lấy xe trong hồ sơ và lọc qua luật tương thích như thường. Khách đã nói rõ xe thì giữ nguyên lời khách; hỏi xe máy thì hồ sơ không giới hạn kết quả. Xe gắn với một cuộc trò chuyện không đổi được (muốn đổi xe thì mở cuộc mới).

**Giao diện React:** `/vehicles` (menu "Xe của tôi"): thêm xe bằng bộ chọn hãng, dòng xe, năm (dùng lại `VehicleModelPicker`), sửa biệt danh hoặc đổi dòng xe, xóa có bước xác nhận, nút "Hỏi AI về xe này" mở chat với xe đó. Trang `/chat`: cuộc mới có ô "Tư vấn cho xe" (mặc định xe mới nhất, có thể chọn "Hỏi chung"); cuộc đã mở hiện "Đang tư vấn cho: ...".

**Kiểm thử:** `advisory/VehicleProfileTest` (11 ca: thêm, xem, sửa, xóa, kiểm tra dữ liệu, quyền sở hữu, giới hạn 5 xe, biệt danh bắt buộc, thêm 8 xe cùng lúc) và `advisory/ChatVehicleProfileTest` (9 ca: dùng xe trong hồ sơ, xe nói rõ thắng hồ sơ, xe máy không bị thu hẹp, xe chưa có phụ tùng tương thích, hồ sơ của người khác, xóa hồ sơ).
