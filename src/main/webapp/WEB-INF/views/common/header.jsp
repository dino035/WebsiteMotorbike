<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<fmt:setLocale value="vi_VN" scope="request"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<nav>
    <a href="${ctx}/products">Sản phẩm</a>

    <%-- Admin: các trang quản trị --%>
    <c:if test="${sessionScope.currentUser.userRole == 'ADMIN'}">
        <a href="${ctx}/products/add">Thêm sản phẩm</a>
        <a href="${ctx}/admin/orders">Quản lý đơn hàng</a>
        <a href="${ctx}/admin/installations">Quản lý lắp đặt</a>
        <a href="${ctx}/admin/warranty">Quản lý bảo hành</a>
        <a href="${ctx}/admin/accounts">Quản lý tài khoản</a>
    </c:if>

    <%-- Chưa đăng nhập --%>
    <c:if test="${empty sessionScope.currentUser}">
        <a href="${ctx}/login">Đăng nhập</a>
        <a href="${ctx}/register">Đăng ký</a>
    </c:if>

    <%-- Đã đăng nhập --%>
    <c:if test="${not empty sessionScope.currentUser}">
        <span><c:out value="${sessionScope.currentUser.email}"/></span>

        <%-- Khách hàng: các trang mua hàng --%>
        <c:if test="${sessionScope.currentUser.userRole != 'ADMIN'}">
            <a href="${ctx}/cart">Giỏ hàng</a>
            <a href="${ctx}/orders">Đơn hàng</a>
            <a href="${ctx}/warranty">Bảo hành</a>
            <a href="${ctx}/addresses">Địa chỉ</a>
        </c:if>

        <a href="${ctx}/notifications">Thông báo</a>
        <a href="${ctx}/logout">Đăng xuất</a>
    </c:if>
</nav>