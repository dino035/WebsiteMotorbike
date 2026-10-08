<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Đăng nhập</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Đăng nhập</h1>

    <p style="color: red;"><c:out value="${error}"/><c:out value="${flashError}"/></p>

    <form action="${pageContext.request.contextPath}/login" method="post">
        <label>Email:</label>
        <input type="email" name="email" required value="<c:out value='${param.email}'/>">

        <label>Mật khẩu:</label>
        <input type="password" name="password" required>

        <input type="submit" value="Đăng nhập">
    </form>

    <p>Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register">Đăng ký</a></p>
</body>
</html>
