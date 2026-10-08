<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Quên mật khẩu</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Quên mật khẩu</h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>
    <c:if test="${not empty error}">
        <p class="msg-error"><c:out value="${error}"/></p>
    </c:if>

    <p>Nhập email bạn đã đăng ký, chúng tôi sẽ gửi liên kết để đặt lại mật khẩu.</p>

    <form action="${pageContext.request.contextPath}/forgot-password" method="post">
        <label>Email:</label>
        <input type="email" name="email" required value="<c:out value='${param.email}'/>">

        <input type="submit" value="Gửi liên kết đặt lại mật khẩu">
    </form>

    <p><a href="${pageContext.request.contextPath}/login">← Quay lại đăng nhập</a></p>
</body>
</html>
