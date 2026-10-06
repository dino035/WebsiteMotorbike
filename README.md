# WebsiteMotorbike

Website mua bán xe máy cũ và phụ tùng, làm bằng **JSP + Servlet** cho môn Lập trình Web.
Tài liệu này là **hợp đồng làm việc chung** giữa hai thành viên: đọc xong có thể viết code theo cùng một kiểu, và đọc code của nhau mà không bị lạ.

> Các mục đánh dấu **[CẦN CHỐT]** là đề xuất chưa được hai bên đồng ý. Chốt xong thì xóa dấu đó.

---

## 1. Công nghệ

| Thành phần | Dùng |
|---|---|
| Ngôn ngữ / nền tảng | Java, Jakarta EE (`jakarta.servlet.*`) |
| Web server | Tomcat 10.1.x |
| Giao diện | JSP + JSTL (`jakarta.tags.core`) + EL, **không dùng scriptlet** `<% %>` |
| ORM | JPA, hiện thực bằng **EclipseLink** (theo yêu cầu giảng viên, không dùng JDBC thuần) |
| CSDL | PostgreSQL |
| Tiện ích | Lombok (`@Getter`, `@Setter`) |
| IDE | IntelliJ IDEA |

Đặc tả lớp (class diagram) đã chốt với giảng viên là nguồn chuẩn cho tên lớp, tên thuộc tính và quan hệ. Không tự đổi tên thuộc tính khác với đặc tả. Muốn đổi thì báo bên kia và cập nhật đặc tả.

---

## 2. Phân chia công việc

### Nhóm 1: Account & Giao dịch

Entity: `User`, `Address`, `ShoppingCart`, `CartItem`, `Order`, `OrderItem`, `InstallationRequest`, `Payment`, `WarrantyRequest`.
Chức năng: đăng ký, đăng nhập, phân quyền, giỏ hàng, đặt hàng, thanh toán, yêu cầu bảo hành.

**Đã bàn giao sẵn từ Nhóm 2:** `User`, `UserRole`, `UserDB`, `RegisterServlet` (đăng ký), `register-form.jsp` và `register-success.jsp`. Từ nay các file này thuộc Nhóm 1, Nhóm 2 không sửa nữa. Nhóm 1 làm tiếp đăng nhập, đăng xuất, phân quyền và giỏ hàng trở đi.

Hai việc cần xử lý trong phần đã bàn giao (xem checklist mục 9):
- Băm mật khẩu (ví dụ BCrypt) trước khi lưu vào `passwordHash`, hiện đang lưu nguyên văn.
- Chuyển `register-form.jsp` vào `WEB-INF/views` và thêm `doGet` cho `/register`.

### Nhóm 2: Sản phẩm & Quản trị

Entity: `Product` (abstract), `Motorbike`, `SparePart`, `VehiclePhoto`, `VehicleConditionAssessment`, `ProductReview`, `Notification`, và các enum `ProductStatus`, `PartOriginType`, `NotificationType`.
Chức năng:
- Quản trị sản phẩm: xem danh sách, thêm, sửa, xóa (xe và phụ tùng), trạng thái sản phẩm, ảnh xe, đánh giá tình trạng xe.
- Phía khách hàng: trang chi tiết sản phẩm, tìm kiếm và lọc sản phẩm, đánh giá sản phẩm.
- Thông báo: lưu và hiển thị thông báo cho người dùng.

> Phân chia này đã được hai bên đồng ý (đã cân lại so với bản đầu: `ProductReview` và `Notification` chuyển sang Nhóm 2, giao thêm trang chi tiết và tìm kiếm).

> Phần AI chatbox / gợi ý tương thích **không thuộc** project web này.

---

## 3. Ranh giới giữa hai nhóm (quan trọng nhất)

Hai nhóm chạm nhau ở ba chỗ: **sản phẩm**, **người dùng đăng nhập** và **thông báo**.

**Quy tắc:**

1. `CartItem` và `OrderItem` chỉ giữ `productId` kiểu `String`, đúng như đặc tả. **Không** đặt `@ManyToOne Product` ở hai lớp này.
2. Khi Nhóm 1 cần thông tin sản phẩm (tên, giá, trạng thái), gọi DAO của Nhóm 2: `ProductDB.selectById(productId)`. Không tự viết lại truy vấn `Product`.
3. Trước khi cho vào giỏ hoặc đặt hàng, kiểm tra `product.getStatus() == ProductStatus.AVAILABLE`.
4. Không sửa file của bên kia. Cần thêm gì ở phần của bên kia thì nhắn hoặc mở issue.
5. `User.userRole` phân biệt khách hàng và admin. Nhóm 2 cần kiểm tra quyền admin cho các trang quản trị, nên Nhóm 1 cung cấp cách đọc user đang đăng nhập bằng cách lưu đối tượng `User` vào session với **tên thuộc tính cố định `"currentUser"`** (đề xuất). Đọc: `User u = (User) session.getAttribute("currentUser");`. Chưa đăng nhập thì giá trị là `null`. (hai bên đã đồng ý tên này).
6. `ProductReview` và `Notification` (Nhóm 2) giữ `accountId` kiểu `String` để biết của người dùng nào, **không** đặt `@ManyToOne User`. Giá trị lấy từ `((User) session.getAttribute("currentUser")).getAccountId()`.
7. Khi cần tạo thông báo (ví dụ đặt hàng thành công, đổi trạng thái đơn), Nhóm 1 gọi DAO của Nhóm 2, không tự lưu bảng `notification`. Chữ ký theo cùng kiểu với các DAO hiện có (`MotorbikeDB.insert(Motorbike)`): `NotificationDB.insert(Notification notification)`. Người gọi tạo `Notification`, đặt `accountId`, `content`, `type`; `isRead` mặc định `false`.

---

## 4. Cấu trúc project

```
src/main/java/com/example/websitemotorbike/
├── model/      # Entity (@Entity) và enum
├── dao/        # Truy cập CSDL qua JPA
└── servlet/    # Controller (@WebServlet)

src/main/webapp/
├── WEB-INF/views/   # Mọi file .jsp (người dùng không gõ URL trực tiếp được)
└── css/style.css    # CSS dùng chung
```

**Luồng một request:**

```
Trình duyệt → Servlet → DAO → CSDL
                 ↓
            JSP (hiển thị)
```

- Servlet nhận request, kiểm tra dữ liệu, gọi DAO, đặt kết quả vào `request.setAttribute(...)`, rồi chuyển sang JSP.
- JSP chỉ hiển thị bằng EL và JSTL. Không có logic nghiệp vụ và không gọi DAO trong JSP.
- DAO là nơi duy nhất làm việc với `EntityManager`.

---

## 5. Quy ước đặt tên

| Loại | Quy ước | Ví dụ |
|---|---|---|
| Entity | Danh từ, theo đúng đặc tả | `Product`, `Motorbike` |
| DAO | `<Entity>DB`, phương thức `static` | `ProductDB`, `MotorbikeDB` |
| Phương thức DAO | `insert`, `update`, `delete`, `selectById`, `selectAll` | `ProductDB.selectById(id)` |
| Servlet | `<Entity><HànhĐộng>Servlet` | `ProductListServlet`, `ProductEditServlet` |
| URL | số nhiều, thường, hành động ở cuối | `/products`, `/products/add`, `/products/edit?id=...`, `/products/delete?id=...` |
| JSP | chữ thường, nối bằng `-` | `product-list.jsp`, `product-edit.jsp` |
| Thuộc tính request | trùng tên biến, số nhiều cho danh sách | `products`, `product`, `error` |

Nhóm 1 đặt tên tương tự: `UserDB`, `OrderDB`, `/login`, `/cart`, `/orders`, `login.jsp`, `cart.jsp`...

---

## 6. Quy ước viết code

### 6.1 Entity

- Dùng Lombok `@Getter` / `@Setter`.
- Khóa chính là `String` sinh UUID (`@Id @GeneratedValue`).
- **Tên field phải trùng tên cột**: field `status` thì cột là `status`. Field `productStatus` sẽ thành cột `productstatus` và gây lỗi lệch dữ liệu.
- Enum luôn dùng `@Enumerated(EnumType.STRING)`, **không dùng** `ORDINAL`.
- Kế thừa dùng `@Inheritance(strategy = InheritanceType.JOINED)` (xem `Product`, `Motorbike`, `SparePart`).
- Enum có nhãn hiển thị tiếng Việt thì để nhãn trong enum (xem `ProductStatus.getLabel()`), không viết `if` trong JSP. Không đổi tên hằng số enum vì CSDL lưu chuỗi này.

### 6.2 Servlet

1. **Kiểm tra dữ liệu ở server.** Kiểm tra ở trình duyệt (`required`, `type="number"`) chỉ để thân thiện, người dùng vẫn bỏ qua được.
2. Đọc tham số, ép kiểu, tạo đối tượng và gọi DAO trong **một khối `try`**.
3. `catch (IllegalArgumentException e)` để bắt lỗi định dạng (`NumberFormatException` và `Enum.valueOf` sai đều là lớp con của nó). Trong `catch`: `setAttribute("error", ...)`, `forward` về form, rồi **`return`**.
4. Dùng `forward` khi báo lỗi (để giữ request attribute), dùng `redirect` sau khi ghi thành công (để F5 không gửi lại form).
5. Sửa dữ liệu thì **tải lại entity đã có** bằng `selectById`, gọi setter rồi `update`. Không `new` một đối tượng id rỗng rồi `merge`, vì sẽ thêm dòng mới thay vì sửa.
6. Tên tham số `request.getParameter("...")` phải trùng thuộc tính `name="..."` trong HTML.
7. Id không tồn tại thì chuyển về trang danh sách, không hiện trang trống.

### 6.3 JSP

- Khai báo taglib: `<%@ taglib uri="jakarta.tags.core" prefix="c" %>`.
- Phần chỉ dành cho một loại (xe hoặc phụ tùng) dùng `<c:if>`. `display:none` của CSS **không** ngăn EL chạy trên server, và truy cập thuộc tính không tồn tại sẽ gây lỗi.
- Thao tác nguy hiểm (xóa) có `onclick="return confirm('...')"`.
- Chọn sẵn giá trị trong `<select>` bằng toán tử 3 ngôi: `${product.status == 'AVAILABLE' ? 'selected' : ''}`.

**Giao diện dùng chung (Nhóm 2 tạo và duy trì):**

- Menu dùng chung nằm ở `src/main/webapp/WEB-INF/views/common/header.jsp`. Mọi trang chèn vào bằng `<jsp:include page="/WEB-INF/views/common/header.jsp" />` ngay sau thẻ `<body>`, không chép lại menu vào từng trang.
- CSS dùng chung ở `src/main/webapp/css/style.css`, nối vào trang bằng `<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">`.
- Menu hiển thị khác nhau theo đăng nhập: `currentUser` là `null` thì hiện "Đăng nhập" / "Đăng ký", khác `null` thì hiện tên và "Đăng xuất". Nhóm 1 cung cấp các trang tương ứng; Nhóm 2 chỉ dùng chúng làm liên kết trong menu. Hiện tại: trang đăng ký là `/register-form.jsp` (servlet `/register` chỉ nhận `POST` khi gửi form, không mở trực tiếp bằng liên kết được). **[CẦN CHỐT: URL trang đăng nhập và đường dẫn đăng xuất, Nhóm 1 cho biết khi làm xong; nếu Nhóm 1 thêm `doGet` cho `/register` thì liên kết "Đăng ký" đổi sang `/register`]**
- Chỉ Nhóm 2 sửa `style.css`. Nhóm 1 cần kiểu riêng cho trang của mình thì tạo file riêng (ví dụ `css/account.css`) để không xung đột khi merge, hoặc nhắn Nhóm 2 thêm vào `style.css`.

### 6.4 CSDL

- EclipseLink **không tự sửa** bảng đã tồn tại. Thêm cột hoặc đổi kiểu thì chạy `ALTER TABLE` bằng tay, và `UPDATE` giá trị cho các dòng cũ.
- Lệnh SQL chạy bằng PostgreSQL (pgAdmin 4). Vì mỗi người có CSDL cục bộ riêng, lệnh chạy xong cũng phải **chép vào `docs/migrations.sql`** và commit cùng PR, kèm một dòng chú thích (ngày, mục đích). Nếu không, bên kia không biết cần chạy gì và sẽ gặp lỗi lệch dữ liệu. Ví dụ:

```sql
-- 2026-10-05: thêm trạng thái sản phẩm
ALTER TABLE product ADD COLUMN status VARCHAR(100);
UPDATE product SET status = 'AVAILABLE';
```

- Tên CSDL và cổng PostgreSQL **giống nhau** ở cả hai máy: tên CSDL `websitemotorbike_db`, cổng `5432` (đã nằm sẵn trong `persistence.example.xml`). Mỗi người tự tạo CSDL tên đó trong pgAdmin 4 trên máy mình. User và mật khẩu thì mỗi máy dùng của riêng mình, chỉ điền trong `persistence.xml` cục bộ.
- **Không commit mật khẩu CSDL.** Xem mục 8.

---

## 7. Quy trình Git

- Nhánh chính: `master`. **Không commit trực tiếp** vào `master`.
- Mỗi tính năng một nhánh: `feature/<tên-ngắn>`, ví dụ `feature/product-management`, `feature/login`.
- Sau khi PR merge, nhánh đó xong. Việc mới thì tạo nhánh mới từ `master` mới nhất:

```
git checkout master
git pull
git checkout -b feature/<tên-mới>
```

- Trước khi commit: `git status` để xem đúng file định đưa lên.
- Chỉ thêm code: `git add src` (tránh kéo theo `.idea/` và file cấu hình).
- Thông điệp commit viết tiếng Anh, ngắn, nói rõ việc đã làm: `Add Product status (AVAILABLE/SOLD) to list, add, edit`.
- Đẩy nhánh và mở Pull Request vào `master` trên GitHub, người còn lại xem qua rồi mới merge.
- Thư mục `.idea/` phải nằm trong `.gitignore`.
- Hai người cùng sửa một file là nguồn xung đột chính. File dùng chung (ví dụ `persistence.xml`, `style.css`, header/footer chung) chỉ sửa sau khi nhắn bên kia.

---

## 8. Cấu hình và bảo mật

- Repo để **private**. Dù vậy, `persistence.xml` **không nằm trong git**, vì mỗi máy có user/mật khẩu PostgreSQL riêng và file chung sẽ gây xung đột mỗi lần pull/merge. Mật khẩu trao đổi qua tin nhắn riêng.
- Repo giữ `src/main/resources/META-INF/persistence.example.xml` với giá trị giả (`YOUR_USER`, `YOUR_PASSWORD`). `persistence.xml` nằm trong `.gitignore`.
- **Thiết lập lần đầu (sau khi clone):** sao chép `persistence.example.xml` thành `persistence.xml` trong cùng thư mục, rồi điền user và mật khẩu thật của máy mình:

```
Copy-Item .\src\main\resources\META-INF\persistence.example.xml .\src\main\resources\META-INF\persistence.xml
```

- **Cảnh báo khi pull commit tách file lần đầu:** nếu máy bạn đã có `persistence.xml` từ trước, hãy chép file ra ngoài project trước khi `git pull`, vì git sẽ xóa nó (không còn được theo dõi). Pull xong thì chép lại.
- Khi sửa `persistence.xml` (thêm thuộc tính, đổi tên CSDL...), **sửa luôn `persistence.example.xml`** và commit để người kia cập nhật theo.
- Lưu ý: mật khẩu cũ vẫn còn trong lịch sử commit trước khi tách. Với repo private và chỉ hai người truy cập thì chấp nhận được; nếu sau này chuyển repo sang public thì phải đổi mật khẩu PostgreSQL.
- Phân quyền admin: các trang thêm/sửa/xóa sản phẩm là trang quản trị. Chức năng kiểm tra quyền sẽ làm sau khi có đăng nhập của Nhóm 1.

---

## 9. Danh sách việc cần chốt chung

- [x] Cách chia việc: `WarrantyRequest` thuộc Nhóm 1; `ProductReview`, `Notification`, trang chi tiết và tìm kiếm thuộc Nhóm 2 (hai bên đã đồng ý)
- [x] Tên thuộc tính session `"currentUser"` (hai bên đã đồng ý)
- [x] Chữ ký phương thức `NotificationDB.insert(Notification)`
- [ ] Phần đăng ký đã bàn giao: băm mật khẩu (BCrypt) và chuyển `register-form.jsp` vào `WEB-INF/views` kèm `doGet`. **[CẦN CHỐT: Nhóm 1 sửa hay Nhóm 2 sửa trước khi bàn giao hẳn]**
- [x] Tên CSDL `websitemotorbike_db`, cổng `5432`
- [x] Tách `persistence.xml` (có `persistence.example.xml`, đã vào `.gitignore`)
- [x] Header/menu và CSS dùng chung: Nhóm 2 tạo (xem mục 6.3)
- [x] Ngôn ngữ giao diện: tiếng Việt (hai bên đã đồng ý)
