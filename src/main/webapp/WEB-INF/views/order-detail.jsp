<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Chi tiết đơn hàng</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Đơn hàng #<c:out value="${order.shortCode}"/></h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>

    <table>
        <tr><th>Ngày đặt</th><td><c:out value="${order.createdAtText}"/></td></tr>
        <tr><th>Trạng thái</th><td><c:out value="${order.status.label}"/></td></tr>
        <tr><th>Người nhận</th><td><c:out value="${order.receiverName}"/> - <c:out value="${order.receiverPhone}"/></td></tr>
        <tr><th>Hình thức nhận hàng</th><td><c:out value="${order.deliveryMethod.label}"/></td></tr>
        <c:if test="${not empty order.shippingAddress}">
            <tr><th>Địa chỉ giao hàng</th><td><c:out value="${order.shippingAddress}"/></td></tr>
        </c:if>
        <c:if test="${not empty payment}">
            <tr><th>Thanh toán</th>
                <td><c:out value="${payment.method.label}"/> - <c:out value="${payment.status.label}"/></td></tr>
        </c:if>
    </table>

    <h2>Sản phẩm</h2>
    <table>
        <tr>
            <th>Sản phẩm</th>
            <th class="num">Đơn giá</th>
            <th>Số lượng</th>
            <th class="num">Thành tiền</th>
        </tr>
        <c:forEach var="item" items="${items}">
            <tr>
                <td><c:out value="${item.productName}"/></td>
                <td class="num"><fmt:formatNumber value="${item.unitPrice}" type="number" groupingUsed="true"/></td>
                <td><c:out value="${item.quantity}"/></td>
                <td class="num"><fmt:formatNumber value="${item.lineTotal}" type="number" groupingUsed="true"/></td>
            </tr>
        </c:forEach>
    </table>
    <p class="cart-total">Tổng cộng: <fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/></p>

    <p><a href="${pageContext.request.contextPath}/orders">← Danh sách đơn hàng</a></p>
</body>
</html>
