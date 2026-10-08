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
    <c:if test="${not empty flashError}">
        <p class="msg-error"><c:out value="${flashError}"/></p>
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
        <c:if test="${not empty installation}">
            <tr><th>Lịch lắp đặt tại cửa hàng</th>
                <td>Ngày <c:out value="${installation.scheduledDateText}"/> - <c:out value="${installation.status.label}"/></td></tr>
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
    <c:if test="${not empty installation}">
        <p style="text-align:right;">Phí công lắp đặt: <fmt:formatNumber value="${installation.laborFee}" type="number" groupingUsed="true"/></p>
    </c:if>
    <p class="cart-total">Tổng cộng: <fmt:formatNumber value="${order.totalAmount}" type="number" groupingUsed="true"/></p>

    <%-- Thanh toán VNPay chưa xong: cho thanh toán (lại) hoặc đổi sang COD (SRS 3.10) --%>
    <c:if test="${not empty payment and payment.method.name() == 'VNPAY' and payment.status.name() != 'PAID' and order.status.name() != 'CANCELLED'}">
        <h2>Thanh toán VNPay</h2>
        <c:choose>
            <c:when test="${payment.status.name() == 'FAILED'}">
                <p class="msg-error">Lần thanh toán gần nhất không thành công. Bạn có thể thanh toán lại hoặc chuyển sang thanh toán khi nhận hàng.</p>
            </c:when>
            <c:otherwise>
                <p class="msg-notice">Đơn hàng chưa được thanh toán.</p>
            </c:otherwise>
        </c:choose>
        <c:if test="${vnpayEnabled}">
            <form class="inline-form" action="${pageContext.request.contextPath}/vnpay/pay" method="post">
                <input type="hidden" name="id" value="<c:out value='${order.orderId}'/>">
                <input type="submit" value="Thanh toán qua VNPay">
            </form>
        </c:if>
        <form class="inline-form" action="${pageContext.request.contextPath}/orders/pay-cod" method="post">
            <input type="hidden" name="id" value="<c:out value='${order.orderId}'/>">
            <input type="submit" value="Chuyển sang thanh toán khi nhận hàng (COD)">
        </form>
    </c:if>

    <c:if test="${not empty attempts}">
        <h2>Lịch sử thanh toán VNPay</h2>
        <table>
            <tr><th>Thời điểm</th><th>Kết quả</th><th>Mã giao dịch VNPay</th></tr>
            <c:forEach var="a" items="${attempts}">
                <tr>
                    <td><c:out value="${a.createdAtText}"/></td>
                    <td><c:out value="${a.status.label}"/></td>
                    <td><c:out value="${a.transactionNo}"/></td>
                </tr>
            </c:forEach>
        </table>
    </c:if>

    <c:if test="${order.status.name() == 'COMPLETED'}">
        <p><a href="${pageContext.request.contextPath}/warranty">Gửi yêu cầu bảo hành cho sản phẩm trong đơn này</a></p>
    </c:if>

    <p><a href="${pageContext.request.contextPath}/orders">← Danh sách đơn hàng</a></p>
</body>
</html>
