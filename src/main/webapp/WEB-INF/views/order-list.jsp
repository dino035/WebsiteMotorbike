<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Đơn hàng của tôi</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Đơn hàng của tôi</h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>
    <c:if test="${not empty flashError}">
        <p class="msg-error"><c:out value="${flashError}"/></p>
    </c:if>

    <c:choose>
        <c:when test="${empty orders}">
            <p>Bạn chưa có đơn hàng nào.</p>
            <p><a href="${pageContext.request.contextPath}/products">Xem danh sách sản phẩm</a></p>
        </c:when>
        <c:otherwise>
            <table>
                <tr>
                    <th>Mã đơn</th>
                    <th>Ngày đặt</th>
                    <th class="num">Tổng tiền</th>
                    <th>Nhận hàng</th>
                    <th>Trạng thái</th>
                </tr>
                <c:forEach var="order" items="${orders}">
                    <tr>
                        <td>
                            <a href="${pageContext.request.contextPath}/orders/detail?id=<c:out value='${order.orderId}'/>">
                                #<c:out value="${order.shortCode}"/>
                            </a>
                        </td>
                        <td><c:out value="${order.createdAtText}"/></td>
                        <td class="num"><fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/></td>
                        <td><c:out value="${order.deliveryMethod.label}"/></td>
                        <td><c:out value="${order.status.label}"/></td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>
</body>
</html>
