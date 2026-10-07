<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Đăng ký thành công</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Đăng ký thành công!</h1>

    <p>Thông tin tài khoản vừa tạo:</p>
    <label>Email:</label>
    <span><c:out value="${user.email}"/></span><br>

    <label>Số điện thoại:</label>
    <span><c:out value="${user.phone}"/></span><br>

    <label>Vai trò:</label>
    <span><c:out value="${user.userRole}"/></span><br>

    <p><a href="${pageContext.request.contextPath}/login">Đăng nhập ngay</a></p>
</body>
</html>
