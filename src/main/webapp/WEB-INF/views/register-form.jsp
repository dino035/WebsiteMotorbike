<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Website Mua Bán Xe Máy &amp; Phụ Tùng</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Đăng ký tài khoản</h1>

    <p style="color: red;"><c:out value="${error}"/></p>

    <form action="${pageContext.request.contextPath}/register" method="post">
        <label>Email:</label>
        <input type="email" name="email" required value="<c:out value='${param.email}'/>">

        <label>Mật khẩu:</label>
        <input type="password" name="password" required>

        <label>Số điện thoại:</label>
        <input type="text" name="phone" required value="<c:out value='${param.phone}'/>">

        <input type = "submit" value = "Đăng ký">

    </form>
</body>
</html>