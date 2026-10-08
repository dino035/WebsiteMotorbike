<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Chi tiết yêu cầu bảo hành (Admin)</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Yêu cầu bảo hành #<c:out value="${warranty.shortCode}"/></h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>
    <c:if test="${not empty flashError}">
        <p class="msg-error"><c:out value="${flashError}"/></p>
    </c:if>

    <table>
        <tr><th>Trạng thái</th><td><c:out value="${warranty.status.label}"/></td></tr>
        <tr><th>Ngày gửi</th><td><c:out value="${warranty.createdAtText}"/></td></tr>
        <tr><th>Tài khoản khách</th>
            <td><c:choose>
                <c:when test="${not empty customer}"><c:out value="${customer.email}"/></c:when>
                <c:otherwise>(không tìm thấy tài khoản)</c:otherwise>
            </c:choose></td></tr>
        <tr><th>Sản phẩm</th><td><c:out value="${warranty.orderItem.productName}"/></td></tr>
        <tr><th>Đơn hàng</th><td>#<c:out value="${warranty.orderItem.order.shortCode}"/>
            (ngày mua <c:out value="${warranty.orderItem.order.createdAtText}"/>)</td></tr>
        <tr><th>Thời hạn bảo hành</th><td><c:out value="${warrantyMonths}"/> tháng, đến <c:out value="${deadlineText}"/></td></tr>
        <tr><th>Mô tả lỗi</th><td><c:out value="${warranty.description}"/></td></tr>
        <tr><th>Minh chứng</th>
            <td><c:choose>
                <c:when test="${not empty warranty.evidenceUrl}">
                    <a href="<c:out value='${warranty.evidenceUrl}'/>" target="_blank" rel="noopener noreferrer"><c:out value="${warranty.evidenceUrl}"/></a>
                </c:when>
                <c:otherwise>(không có)</c:otherwise>
            </c:choose></td></tr>
        <c:if test="${warranty.status.name() != 'PENDING'}">
            <tr><th>Xử lý lúc</th><td><c:out value="${warranty.resolvedAtText}"/></td></tr>
            <tr><th>Ghi chú / lý do</th><td><c:out value="${warranty.adminNote}"/></td></tr>
        </c:if>
    </table>

    <h2>Xử lý yêu cầu</h2>
    <c:choose>
        <c:when test="${warranty.status.name() == 'PENDING'}">
            <form action="${pageContext.request.contextPath}/admin/warranty/decide" method="post">
                <input type="hidden" name="id" value="<c:out value='${warranty.warrantyId}'/>">
                <label>Ghi chú (bắt buộc khi từ chối):</label>
                <textarea name="note" rows="3" maxlength="500" style="width:100%;box-sizing:border-box;"></textarea>
                <button type="submit" name="decision" value="APPROVE"
                        onclick="return confirm('Duyệt yêu cầu và thay mới sản phẩm?')">Duyệt (thay mới)</button>
                <button type="submit" name="decision" value="REJECT"
                        onclick="return confirm('Từ chối yêu cầu bảo hành này?')">Từ chối</button>
            </form>
        </c:when>
        <c:otherwise>
            <p>Yêu cầu đã được xử lý, không thể thay đổi.</p>
        </c:otherwise>
    </c:choose>

    <p><a href="${pageContext.request.contextPath}/admin/warranty">← Danh sách yêu cầu bảo hành</a></p>
</body>
</html>
