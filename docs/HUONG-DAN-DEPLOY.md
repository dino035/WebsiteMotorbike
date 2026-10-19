# Hướng dẫn đưa website lên hosting (Render)

Website chạy bằng Docker (file `Dockerfile` ở thư mục gốc). Hướng dẫn dùng **Render** vì có gói miễn phí và hỗ trợ Docker.
Cơ sở dữ liệu vẫn là Supabase đang dùng chung, **không cần đổi gì ở Supabase**.

## 1. Cách hoạt động (để hiểu mình đang làm gì)

- `Dockerfile` biên dịch dự án thành file WAR rồi đặt vào Tomcat 10.1.
- Các file chứa mật khẩu (`persistence.xml`, `vnpay.properties`, `mail.properties`) **không nằm trong Git** nên không có trong ảnh Docker. Bạn nhập chúng vào Render dưới dạng **Secret Files**; mỗi lần khởi động, `docker-entrypoint.sh` chép chúng vào ứng dụng.
- Các biến `SUPABASE_URL`, `SUPABASE_BUCKET`, `SUPABASE_KEY` (ảnh sản phẩm) nhập ở mục **Environment Variables**.
- Ứng dụng chạy giờ Việt Nam (đã đặt trong `Dockerfile`) nên giờ đặt hàng và báo cáo theo ngày đúng.

## 2. Chuẩn bị

1. Merge nhánh có `Dockerfile`, `docker-entrypoint.sh`, `.dockerignore` vào `master`.
2. Tài khoản Render (đăng nhập bằng GitHub). Tài khoản GitHub cần có quyền truy cập repo `WebsiteMotorbike`; nếu repo thuộc người khác thì **chủ repo** nên là người tạo dịch vụ trên Render (hoặc repo phải ở chế độ công khai).
3. Có sẵn: nội dung file `persistence.xml` thật (bản trên máy bạn), TmnCode và HashSecret của VNPay sandbox, khóa Supabase Storage.

## 3. Tạo dịch vụ trên Render

1. Dashboard Render → **New +** → **Web Service** → chọn repo `WebsiteMotorbike`.
2. Điền:
   - **Name**: đặt tên dễ nhớ, ví dụ `motorbike-shop`. Địa chỉ web sẽ là `https://motorbike-shop.onrender.com` (nếu tên chưa ai dùng).
   - **Region**: Singapore (gần Supabase nhất).
   - **Branch**: `master`.
   - **Runtime/Language**: **Docker** (Render tự tìm `Dockerfile` ở thư mục gốc).
   - **Instance Type**: Free.
3. **Environment Variables** (bấm Add Environment Variable):

   | Key | Value |
   |---|---|
   | `SUPABASE_URL` | `https://<mã-dự-án>.supabase.co` |
   | `SUPABASE_BUCKET` | `photos` |
   | `SUPABASE_KEY` | khóa `sb_secret_...` (giữ bí mật) |

   Không cần đặt `PORT`: Render tự đặt và `docker-entrypoint.sh` tự đọc.
4. **Secret Files** (mục Environment, phần Secret Files → Add Secret File):

   | Filename | Nội dung |
   |---|---|
   | `persistence.xml` | **bắt buộc**: dán nguyên nội dung file `src/main/resources/META-INF/persistence.xml` trên máy bạn (đã điền Supabase) |
   | `vnpay.properties` | tùy chọn: xem mẫu dưới đây |
   | `mail.properties` | tùy chọn, để gửi email quên mật khẩu: xem mục 5 (dùng Brevo) |

   Mẫu `vnpay.properties` (thay bằng giá trị của bạn và đúng tên dịch vụ):
   ```
   vnpay.tmnCode=<TmnCode>
   vnpay.hashSecret=<HashSecret>
   vnpay.payUrl=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
   vnpay.returnUrl=https://motorbike-shop.onrender.com/vnpay/return
   ```
5. Bấm **Create Web Service**. Lần build đầu mất vài phút; xem tiến trình ở tab **Logs**.
6. Khi Logs có dòng `Server startup in ...`, mở địa chỉ `https://<tên>.onrender.com`.

## 4. Cấu hình VNPay sau khi có địa chỉ

- `vnpay.returnUrl` phải đúng `https://<tên>.onrender.com/vnpay/return`.
- Đăng nhập trang quản lý của VNPay sandbox, vào phần cấu hình **IPN URL** của terminal và điền `https://<tên>.onrender.com/vnpay/ipn`. Trên máy chủ công khai thì VNPay gọi được IPN, nên kết quả được ghi nhận kể cả khi khách đóng trình duyệt trước khi quay về.
- Sửa Secret File xong thì bấm **Manual Deploy → Restart** (hoặc Deploy latest commit) để áp dụng.

## 5. Gửi email quên mật khẩu bằng Brevo (tùy chọn)

Brevo có gói miễn phí (khoảng 300 email mỗi ngày). Ứng dụng gọi API của Brevo qua HTTPS nên chạy được trên Render.

1. Đăng ký tài khoản ở brevo.com (có thể dùng email của bạn).
2. Xác minh người gửi: vào **Senders, Domains & Dedicated IPs → Senders → Add a sender**, nhập tên và một địa chỉ email của bạn, rồi bấm liên kết xác nhận Brevo gửi vào hộp thư đó. Địa chỉ này sẽ là người gửi của mọi email, **bắt buộc đã xác minh**, nếu không Brevo từ chối gửi.
3. Tạo khóa API: **SMTP & API → API Keys → Generate a new API key**, sao chép khóa (chỉ hiện một lần). Đây là bí mật, **không dán vào chat, Git hay tài liệu**.
4. Trên Render, sửa Secret File `mail.properties` (tạo mới nếu chưa có) với nội dung sau, thay bằng giá trị của bạn:
   ```
   mail.brevo.apiKey=<khóa API vừa tạo>
   mail.from=<email người gửi đã xác minh ở bước 2>
   mail.fromName=Motorbike Shop
   app.baseUrl=https://<tên dịch vụ>.onrender.com
   ```
   `app.baseUrl` là địa chỉ web thật của bạn (không có dấu `/` ở cuối), dùng để tạo liên kết đặt lại mật khẩu trong email.
5. Lưu Secret File, Render tự khởi động lại. Thử bằng chức năng **Quên mật khẩu**, email sẽ về hộp thư của tài khoản.

Nếu không nhận được email: xem tab **Logs** trên Render, dòng `Gửi email qua Brevo thất bại: HTTP 401/400 ...` nêu lý do (sai khóa API, người gửi chưa xác minh). Kiểm tra cả thư mục Spam.

## 6. Cập nhật phiên bản mới

Mỗi lần push hoặc merge vào `master`, Render tự build lại và triển khai. Xem tiến trình ở tab **Events** hoặc **Logs**.

## 7. Giới hạn của gói miễn phí cần biết

- Dịch vụ **ngủ sau khoảng 15 phút không có người truy cập**; lần mở đầu tiên sau đó mất khoảng nửa đến một phút để khởi động lại. Nên mở web trước khi trình bày.
- Phiên đăng nhập mất khi dịch vụ khởi động lại; khách phải đăng nhập lại.
- Bộ nhớ 512 MB. Nếu Logs báo hết bộ nhớ (OutOfMemory), nâng gói hoặc báo lại để giảm cấu hình.
- Gói Supabase miễn phí tự tạm dừng sau khoảng 7 ngày không hoạt động; nếu web báo không kết nối được CSDL thì vào Supabase bấm Restore/Resume project.
- Render gói miễn phí **chặn các cổng SMTP** (25, 465, 587) nên không gửi email bằng SMTP được. Vì vậy ứng dụng gửi qua **API của Brevo** (HTTPS cổng 443), xem mục 5. Chưa cấu hình thì chức năng quên mật khẩu vẫn chạy ở chế độ phát triển (liên kết đặt lại được in trong Logs).

## 8. Khi gặp lỗi

| Hiện tượng | Nguyên nhân thường gặp |
|---|---|
| Logs: `thieu persistence.xml` | Chưa tạo Secret File `persistence.xml` hoặc gõ sai tên |
| Logs báo không kết nối được CSDL | Sai thông tin trong `persistence.xml`; dự án Supabase đang tạm dừng; dùng nhầm cổng 6543 (phải là Session pooler cổng 5432, có `?sslmode=require`) |
| Trang trả 502 hoặc không mở được ngay sau khi tạo | Còn đang build hoặc khởi động; xem Logs |
| Ô VNPay ở trang thanh toán bị khóa | Chưa có `vnpay.properties` hoặc còn giá trị `YOUR_...` |
| Thanh toán xong báo "Chữ ký không hợp lệ" | `vnpay.hashSecret` hoặc `vnpay.tmnCode` sai; `vnpay.returnUrl` không đúng địa chỉ web |
| Thêm ảnh báo thiếu biến `SUPABASE_...` | Thiếu hoặc sai tên biến môi trường |

## 9. Chạy thử bản Docker trên máy mình (tùy chọn)

Cần Docker Desktop đang chạy. Tạo thư mục `secrets` (đừng commit) chứa `persistence.xml` trỏ tới CSDL, rồi:

```
docker build -t motorbike .
docker run --rm -p 8080:8080 -v "%cd%\secrets:/etc/secrets:ro" -e PORT=8080 motorbike
```

Mở `http://localhost:8080`. Trong `persistence.xml` dùng `host.docker.internal` thay cho `localhost` nếu CSDL chạy trên máy bạn.
