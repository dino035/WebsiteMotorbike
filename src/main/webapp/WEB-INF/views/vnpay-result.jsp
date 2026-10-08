<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Kết quả thanh toán VNPay</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Kết quả thanh toán VNPay</h1>

    <c:choose>
        <c:when test="${not empty problem}">
            <p class="msg-error"><c:out value="${problem}"/></p>
            <p><a href="${pageContext.request.contextPath}/orders">Xem danh sách đơn hàng</a></p>
        </c:when>
        <c:otherwise>
            <c:choose>
                <c:when test="${outcome == 'PAID'}">
                    <p class="msg-ok">Thanh toán thành công cho đơn hàng #<c:out value="${order.shortCode}"/>.</p>
                </c:when>
                <c:when test="${outcome == 'ALREADY_PROCESSED' and attempt.status.name() == 'PAID'}">
                    <p class="msg-ok">Giao dịch này đã được ghi nhận thành công trước đó (đơn hàng #<c:out value="${order.shortCode}"/>).</p>
                </c:when>
                <c:when test="${outcome == 'ALREADY_PROCESSED'}">
                    <p class="msg-error">Giao dịch này đã được ghi nhận là không thành công trước đó (đơn hàng #<c:out value="${order.shortCode}"/>).</p>
                </c:when>
                <c:when test="${outcome == 'PAID_ORDER_CANCELLED' or outcome == 'PAID_DUPLICATE'}">
                    <p class="msg-error">Cửa hàng đã nhận tiền của bạn nhưng không thể áp dụng cho đơn hàng
                        #<c:out value="${order.shortCode}"/>
                        (<c:choose><c:when test="${outcome == 'PAID_DUPLICATE'}">đơn đã được thanh toán trước đó</c:when><c:otherwise>đơn đã bị hủy</c:otherwise></c:choose>).
                        Cửa hàng sẽ liên hệ để hoàn tiền.</p>
                </c:when>
                <c:when test="${outcome == 'FAILED'}">
                    <p class="msg-error">Thanh toán không thành công: <c:out value="${reason}"/>.</p>
                    <p>Đơn hàng #<c:out value="${order.shortCode}"/> vẫn được giữ. Bạn có thể thanh toán lại bằng VNPay
                        hoặc chuyển sang thanh toán khi nhận hàng (COD) ở trang chi tiết đơn.</p>
                </c:when>
                <c:when test="${outcome == 'AMOUNT_MISMATCH'}">
                    <p class="msg-error">Số tiền không khớp với đơn hàng, giao dịch không được ghi nhận.</p>
                </c:when>
                <c:otherwise>
                    <p class="msg-error">Không tìm thấy giao dịch này.</p>
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty attempt and (outcome == 'PAID' or outcome == 'ALREADY_PROCESSED')}">
                <table>
                    <tr><th>Mã giao dịch VNPay</th><td><c:out value="${attempt.transactionNo}"/></td></tr>
                    <tr><th>Ngân hàng</th><td><c:out value="${attempt.bankCode}"/></td></tr>
                    <tr><th>Số tiền</th><td><fmt:formatNumber value="${attempt.amount}" type="number" groupingUsed="true"/></td></tr>
                </table>
            </c:if>

            <c:if test="${not empty order}">
                <p><a href="${pageContext.request.contextPath}/orders/detail?id=<c:out value='${order.orderId}'/>">Xem chi tiết đơn hàng</a></p>
            </c:if>
        </c:otherwise>
    </c:choose>
</body>
</html>
