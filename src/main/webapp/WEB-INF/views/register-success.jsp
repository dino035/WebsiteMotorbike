<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Đăng ký thành công</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <h1>Đăng ký thành công!</h1>

    <p>Thông tin tài khoản vừa tạo:</p>
    <label>Email:</label>
    <span>${user.email}</span><br>

    <label>Phone:</label>
    <span>${user.phone}</span><br>

    <label>Vai trò:</label>
    <span>${user.userRole}</span><br>

    <a href="register-form.jsp">Quay lại trang đăng ký</a>
</body>
</html>
