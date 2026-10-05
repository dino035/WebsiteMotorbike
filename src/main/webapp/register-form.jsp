<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Website Mua Bán Xe Máy & Phụ Tùng</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <h1>Đăng ký tài khoản</h1>

    <form action = "register" method = "post">
        <label>Email:</label>
        <input type = "email" name = "email" required><br>

        <label>Password:</label>
        <input type = "password" name = "password" required><br>

        <label>Phone:</label>
        <input type = "text" name = "phone" required><br>

        <input type = "submit" value = "Đăng ký">

        <p>${error}</p>
    </form>
</body>
</html>