<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Đặt lại mật khẩu</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Đặt lại mật khẩu</h1>

    <c:if test="${not empty error}">
        <p class="msg-error"><c:out value="${error}"/></p>
    </c:if>

    <c:choose>
        <c:when test="${invalid}">
            <p><a href="${pageContext.request.contextPath}/forgot-password">Yêu cầu liên kết mới</a></p>
        </c:when>
        <c:otherwise>
            <form action="${pageContext.request.contextPath}/reset-password" method="post">
                <input type="hidden" name="token" value="<c:out value='${token}'/>">

                <label>Mật khẩu mới (ít nhất 6 ký tự):</label>
                <input type="password" name="password" required minlength="6" maxlength="72">

                <label>Nhập lại mật khẩu mới:</label>
                <input type="password" name="confirmPassword" required minlength="6" maxlength="72">

                <input type="submit" value="Đặt lại mật khẩu">
            </form>
        </c:otherwise>
    </c:choose>
</body>
</html>
