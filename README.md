# WebsiteMotorbike

Website mua bán **xe máy cũ** và **phụ tùng xe máy**. Đây là bài tập lớn môn Lập trình Web, làm bằng **JSP + Servlet** (Java).

**Dùng thử ngay, không cần cài gì:** https://motorbike-shop-7td0.onrender.com

> Web đặt trên gói hosting miễn phí nên **lần mở đầu có thể phải chờ 30–60 giây** để nó tự bật lại. Nếu thấy lỗi 500 hoặc "không kết nối được cơ sở dữ liệu", xem mục [Gặp lỗi thì làm gì](#6-gặp-lỗi-thì-làm-gì).

**Nhóm thực hiện:** Nguyễn Hồng Đào, Nguyễn Khôi Nguyên

---

## Mục lục

1. [Web này làm được gì](#1-web-này-làm-được-gì)
2. [Cách dùng thử trên web demo](#2-cách-dùng-thử-trên-web-demo)
3. [Công nghệ dùng](#3-công-nghệ-dùng)
4. [Tự chạy web trên máy của bạn](#4-tự-chạy-web-trên-máy-của-bạn)
5. [Cấu trúc code](#5-cấu-trúc-code)
6. [Gặp lỗi thì làm gì](#6-gặp-lỗi-thì-làm-gì)
7. [Tài liệu khác trong repo](#7-tài-liệu-khác-trong-repo)

---

## 1. Web này làm được gì

Web có hai kiểu người dùng: **khách hàng** (người mua) và **quản trị viên** (admin, người bán).

### Khách hàng

| Chức năng | Mô tả |
|---|---|
| Đăng ký, đăng nhập | Mật khẩu được mã hóa trước khi lưu |
| Quên mật khẩu | Nhận liên kết đặt lại mật khẩu qua email |
| Xem và tìm sản phẩm | Lọc theo loại (xe máy / phụ tùng), tìm theo tên và giá |
| Xem chi tiết sản phẩm | Ảnh, thông tin, điểm đánh giá tình trạng xe |
| So sánh sản phẩm | Đặt nhiều sản phẩm cạnh nhau để so |
| Giỏ hàng và đặt hàng | Thêm, đổi số lượng, xóa, rồi đặt hàng |
| Thanh toán | Trả tiền mặt khi nhận hàng (COD), hoặc trả online bằng VNPay (môi trường thử, không mất tiền thật) |
| Sổ địa chỉ và hồ sơ xe | Lưu địa chỉ nhận hàng và thông tin chiếc xe của mình |
| Yêu cầu bảo hành, lắp đặt | Gửi yêu cầu và xem đã được xử lý chưa |
| Đánh giá sản phẩm | Chỉ người đã mua mới được đánh giá, mỗi người một lần cho mỗi sản phẩm |
| Thông báo | Xem thông báo về đơn hàng, bảo hành |

### Quản trị viên

| Chức năng | Mô tả |
|---|---|
| Quản lý sản phẩm | Thêm, sửa, xóa xe và phụ tùng, tải ảnh lên, chấm điểm tình trạng xe |
| Quản lý đơn hàng | Xem đơn, đổi trạng thái, xác nhận đã thu tiền |
| Quản lý bảo hành, lắp đặt | Xem và xử lý yêu cầu của khách |
| Quản lý tài khoản | Khóa hoặc mở khóa tài khoản |
| Báo cáo | Thống kê doanh thu |

---

## 2. Cách dùng thử trên web demo

1. Mở https://motorbike-shop-7td0.onrender.com
2. Bấm **Đăng ký** để tạo tài khoản khách hàng của bạn, rồi **Đăng nhập**.
3. Vào **Sản phẩm**, bấm vào một sản phẩm để xem chi tiết, bấm thêm vào giỏ.
4. Mở **Giỏ hàng** → **Đặt hàng**. Chọn thanh toán COD để thử nhanh.
5. Xem đơn vừa đặt ở mục **Đơn hàng**.

Các trang quản trị (đường dẫn bắt đầu bằng `/admin`) chỉ tài khoản admin mới vào được. Tài khoản bạn tự đăng ký luôn là khách hàng. Muốn xem phần admin, hãy liên hệ nhóm để được cấp tài khoản.

---

## 3. Công nghệ dùng

| Thành phần | Dùng |
|---|---|
| Ngôn ngữ | Java 17 trở lên |
| Web server | Apache Tomcat 10.1 |
| Giao diện | JSP + JSTL (không viết code Java trong file JSP) |
| Làm việc với cơ sở dữ liệu | JPA (EclipseLink), theo yêu cầu của giảng viên |
| Cơ sở dữ liệu | PostgreSQL |
| Lưu ảnh sản phẩm | Supabase Storage |
| Mã hóa mật khẩu | jBCrypt |
| Đóng gói và đưa lên mạng | Docker, chạy trên Render |

---

## 4. Tự chạy web trên máy của bạn

Nếu chỉ muốn xem web thì dùng bản demo ở trên là đủ. Mục này dành cho ai muốn chạy code trên máy mình.

> **Lưu ý quan trọng:** cơ sở dữ liệu của bản demo là của nhóm, **không công khai**. Khi chạy trên máy, bạn cần có cơ sở dữ liệu PostgreSQL của riêng mình (cách làm ở bước 3). Web sẽ tự tạo các bảng ở lần chạy đầu, nhưng ban đầu chưa có sản phẩm nào.

### Bước 1. Cài sẵn

- JDK 17 trở lên
- Apache Tomcat **10.1** (phải là bản 10 trở lên, **không dùng Tomcat 9**, vì dự án dùng `jakarta.servlet`)
- IntelliJ IDEA Ultimate (chạy Tomcat trực tiếp được). Nếu dùng bản Community thì cài thêm plugin **Smart Tomcat**.
- Git
- PostgreSQL (cài trên máy), hoặc một tài khoản Supabase miễn phí (supabase.com)

### Bước 2. Lấy code về

```
git clone https://github.com/dino035/WebsiteMotorbike.git
cd WebsiteMotorbike
```

Mở thư mục này bằng IntelliJ và đợi nó tải xong thư viện (Maven).

### Bước 3. Tạo cơ sở dữ liệu và file cấu hình

1. Tạo một cơ sở dữ liệu PostgreSQL trống. Có hai cách:
   - **Cài PostgreSQL trên máy**, tạo database tên `websitemotorbike`.
   - **Tạo dự án Supabase miễn phí**, rồi lấy thông tin kết nối ở nút **Connect** (chọn **Session pooler**).
2. Vào thư mục `src/main/resources/META-INF/`, sao chép `persistence.example.xml` thành `persistence.xml` (cùng thư mục). Trên Windows PowerShell:
   ```
   Copy-Item .\src\main\resources\META-INF\persistence.example.xml .\src\main\resources\META-INF\persistence.xml
   ```
3. Mở `persistence.xml` và sửa 3 dòng cho đúng cơ sở dữ liệu của bạn:

   | Thuộc tính | Cơ sở dữ liệu trên máy | Supabase |
   |---|---|---|
   | `jakarta.persistence.jdbc.url` | `jdbc:postgresql://localhost:5432/websitemotorbike` | `jdbc:postgresql://<host>:5432/postgres?sslmode=require` |
   | `jakarta.persistence.jdbc.user` | `postgres` | `postgres.<mã-dự-án>` |
   | `jakarta.persistence.jdbc.password` | mật khẩu PostgreSQL của bạn | mật khẩu bạn đặt khi tạo dự án |

   Với Supabase, dùng cổng **5432** (Session pooler), không dùng cổng 6543, và giữ đuôi `?sslmode=require`.
4. Giữ nguyên dòng `schema-generation.database.action` là `create`. Với giá trị này, web tự tạo bảng còn thiếu và không xóa dữ liệu có sẵn.

`persistence.xml` đã được Git bỏ qua, nên mật khẩu của bạn sẽ không bị đưa lên mạng khi bạn commit.

### Bước 4. Chạy

1. Trong IntelliJ, tạo cấu hình chạy **Tomcat Server → Local**, chọn Tomcat 10.1 của bạn, và thêm artifact của dự án để triển khai.
2. Bấm **Run**.
3. Mở trình duyệt vào `http://localhost:8080/` (hoặc đường dẫn IntelliJ hiển thị).

### Bước 5. Tạo tài khoản admin (nếu cần xem phần quản trị)

1. Đăng ký một tài khoản bình thường trên web.
2. Mở công cụ quản lý cơ sở dữ liệu (pgAdmin, DBeaver, hoặc SQL Editor của Supabase) và chạy:
   ```sql
   UPDATE users SET userrole = 'ADMIN' WHERE email = 'email-bạn-vừa-đăng-ký';
   ```
3. Đăng xuất rồi đăng nhập lại. Menu sẽ có thêm các mục quản trị.

### Các phần tùy chọn (không làm cũng chạy được)

| Chức năng | Nếu không cấu hình thì sao | Cách cấu hình |
|---|---|---|
| Tải ảnh sản phẩm lên | Thêm hoặc sửa ảnh sẽ báo lỗi | Tạo bucket tên `photos` (bật Public) trong Supabase Storage, rồi đặt 3 biến môi trường trong cấu hình chạy Tomcat: `SUPABASE_URL` (dạng `https://<mã-dự-án>.supabase.co`), `SUPABASE_BUCKET` (`photos`), `SUPABASE_KEY` (khóa `service_role` của dự án của bạn, giữ bí mật) |
| Gửi email quên mật khẩu | Web không gửi email, mà in liên kết đặt lại mật khẩu ra cửa sổ Console của Tomcat | Sao chép `src/main/resources/mail.example.properties` thành `mail.properties` cùng thư mục rồi điền thông tin |
| Thanh toán VNPay | Nút VNPay bị khóa, chỉ chọn được COD | Đăng ký tài khoản thử ở sandbox.vnpayment.vn, sao chép `src/main/resources/vnpay.example.properties` thành `vnpay.properties` rồi điền thông tin |

Các file `mail.properties`, `vnpay.properties` cũng đã được Git bỏ qua.

---

## 5. Cấu trúc code

```
src/main/java/com/example/websitemotorbike/
├── model/      # Các lớp dữ liệu, mỗi lớp ứng với một bảng: Product, Order, User...
├── dao/        # Các lớp đọc/ghi cơ sở dữ liệu (ProductDB, OrderDB...)
├── servlet/    # Nhận yêu cầu từ trình duyệt và xử lý (ProductListServlet, CartServlet...)
├── filter/     # Chặn truy cập: AdminFilter (không phải admin thì báo lỗi 403), LoginRequiredFilter
└── util/       # Hàm dùng chung: kiểm tra dữ liệu nhập, gửi email, VNPay, upload ảnh...

src/main/webapp/
├── WEB-INF/views/   # Tất cả file giao diện .jsp
└── css/style.css    # CSS dùng chung
```

**Một yêu cầu đi qua web như thế nào:**

```
Trình duyệt  →  Servlet  →  DAO  →  Cơ sở dữ liệu
                   ↓
              JSP (hiển thị kết quả)
```

1. Người dùng bấm một đường dẫn, ví dụ `/products`.
2. **Servlet** nhận yêu cầu, kiểm tra dữ liệu, rồi nhờ **DAO** lấy dữ liệu từ cơ sở dữ liệu.
3. Servlet chuyển dữ liệu sang **JSP** để vẽ ra trang web.

---

## 6. Gặp lỗi thì làm gì

| Hiện tượng | Nguyên nhân và cách xử lý |
|---|---|
| Mở bản demo thấy **lỗi 500** hoặc báo `FATAL: tenant/user ... not found` | Cơ sở dữ liệu miễn phí tự tạm dừng khi lâu không dùng. Nếu bạn là người trong nhóm, vào Supabase bấm **Resume project**, đợi vài phút rồi khởi động lại web. Nếu bạn là người ngoài, vui lòng báo cho nhóm. |
| Mở bản demo rất chậm lần đầu | Bình thường. Web đang tự bật lại, đợi khoảng 1 phút. |
| Chạy trên máy báo thiếu `persistence.xml` | Chưa làm bước 3. Sao chép file mẫu và điền thông tin. |
| Lỗi `ClassNotFoundException` hoặc 404 ngay khi bật Tomcat | Đang dùng Tomcat 9. Đổi sang Tomcat 10.1. |
| Báo không kết nối được cơ sở dữ liệu (chạy trên máy) | Kiểm tra lại URL, user, password trong `persistence.xml`. Với Supabase thì dùng cổng 5432 và có `?sslmode=require`. |
| Thêm ảnh sản phẩm báo thiếu biến `SUPABASE_...` | Chưa đặt biến môi trường ở phần "Các phần tùy chọn". |

---

## 7. Tài liệu khác trong repo

Dành cho những ai muốn tìm hiểu sâu hơn:

| File | Nội dung |
|---|---|
| `docs/HUONG-DAN-DEPLOY.md` | Cách đưa web lên hosting Render |
| `docs/QUY-UOC-LAM-VIEC.md` | Quy ước viết code và cách làm việc nhóm |
| `docs/migrations.sql` | Các lệnh SQL đã chạy để sửa cơ sở dữ liệu |
