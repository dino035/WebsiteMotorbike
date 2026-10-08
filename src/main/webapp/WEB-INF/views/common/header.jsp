<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<nav>
    <a href="${pageContext.request.contextPath}/products">Sản phẩm</a>

    <c:if test="${sessionScope.currentUser.userRole == 'ADMIN'}">
        <a href="${pageContext.request.contextPath}/products/add">Thêm sản phẩm</a>
        <a href="${pageContext.request.contextPath}/admin/orders">Quản lý đơn hàng</a>
    </c:if>

    <c:if test="${empty sessionScope.currentUser}">
        <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
        <a href="${pageContext.request.contextPath}/register">Đăng ký</a>
    </c:if>

    <c:if test="${not empty sessionScope.currentUser}">
        <span><c:out value="${sessionScope.currentUser.email}"/></span>
        <a href="${pageContext.request.contextPath}/cart">Giỏ hàng</a>
        <a href="${pageContext.request.contextPath}/orders">Đơn hàng</a>
        <a href="${pageContext.request.contextPath}/notifications">Thông báo</a>
        <a href="${pageContext.request.contextPath}/logout">Đăng xuất</a>
    </c:if>
</nav>