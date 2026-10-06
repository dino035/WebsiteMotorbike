<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<nav>
    <a href="${pageContext.request.contextPath}/products">Sản phẩm</a>

    <c:if test="${sessionScope.currentUser.userRole == 'ADMIN'}">
        <a href="${pageContext.request.contextPath}/products/add">Thêm sản phẩm</a>
    </c:if>

    <c:if test="${empty sessionScope.currentUser}">
        <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
        <a href="${pageContext.request.contextPath}/register">Đăng ký</a>
    </c:if>

    <c:if test="${not empty sessionScope.currentUser}">
        <span><c:out value="${sessionScope.currentUser.email}"/></span>
        <a href="${pageContext.request.contextPath}/logout">Đăng xuất</a>
    </c:if>
</nav>