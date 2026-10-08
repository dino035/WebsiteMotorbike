<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Thanh toán</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Thanh toán</h1>

    <c:if test="${not empty error}">
        <p class="msg-error"><c:out value="${error}"/></p>
    </c:if>
    <c:forEach var="notice" items="${notices}">
        <p class="msg-notice"><c:out value="${notice}"/></p>
    </c:forEach>

    <h2>Đơn hàng của bạn</h2>
    <table>
        <tr>
            <th>Sản phẩm</th>
            <th class="num">Đơn giá</th>
            <th>Số lượng</th>
            <th class="num">Thành tiền</th>
        </tr>
        <c:forEach var="line" items="${lines}">
            <tr>
                <td><c:out value="${line.name}"/></td>
                <td class="num"><fmt:formatNumber value="${line.unitPrice}" type="number" groupingUsed="true"/></td>
                <td><c:out value="${line.quantity}"/></td>
                <td class="num"><fmt:formatNumber value="${line.lineTotal}" type="number" groupingUsed="true"/></td>
            </tr>
        </c:forEach>
    </table>
    <p class="cart-total">Tổng cộng: <fmt:formatNumber value="${total}" type="number" groupingUsed="true"/></p>
    <p><a href="${pageContext.request.contextPath}/cart">← Quay lại giỏ hàng</a></p>

    <h2>Thông tin nhận hàng</h2>
    <form action="${pageContext.request.contextPath}/checkout" method="post">
        <label>Họ tên người nhận:</label>
        <input type="text" name="receiverName" required maxlength="100" value="<c:out value='${param.receiverName}'/>">

        <label>Số điện thoại:</label>
        <input type="text" name="receiverPhone" required value="<c:out value='${empty param.receiverPhone ? defaultPhone : param.receiverPhone}'/>">

        <label>Hình thức nhận hàng:</label>
        <c:forEach var="m" items="${deliveryMethods}">
            <%-- Lắp đặt tại cửa hàng chỉ hiện khi đơn có ít nhất một phụ tùng --%>
            <c:if test="${m.name() != 'INSTALL' or hasSparePart}">
                <div>
                    <input type="radio" name="deliveryMethod" id="dm-${m}" value="${m}"
                           ${(empty param.deliveryMethod ? 'DELIVERY' : param.deliveryMethod) == m.name() ? 'checked' : ''}>
                    <label for="dm-${m}" style="display:inline;font-weight:normal;"><c:out value="${m.label}"/></label>
                </div>
            </c:if>
        </c:forEach>

        <label>Địa chỉ giao hàng (bắt buộc khi chọn giao tận nơi):</label>
        <textarea name="shippingAddress" rows="3" maxlength="500" style="width:100%;box-sizing:border-box;"><c:out value="${param.shippingAddress}"/></textarea>

        <c:if test="${hasSparePart}">
            <label>Ngày hẹn lắp đặt (bắt buộc khi chọn lắp đặt tại cửa hàng):</label>
            <input type="date" name="installDate" min="${minInstallDate}" max="${maxInstallDate}"
                   value="<c:out value='${param.installDate}'/>">
            <p class="msg-notice">Lắp đặt tại cửa hàng có thêm phí công
                <fmt:formatNumber value="${laborFee}" type="number" groupingUsed="true"/> (cộng vào tổng đơn),
                tối đa ${maxPerDay} lượt mỗi ngày.</p>
        </c:if>

        <label>Phương thức thanh toán:</label>
        <c:forEach var="pm" items="${paymentMethods}">
            <div>
                <input type="radio" name="paymentMethod" id="pm-${pm}" value="${pm}"
                       ${pm.name() == 'VNPAY' ? 'disabled' : 'checked'}>
                <label for="pm-${pm}" style="display:inline;font-weight:normal;">
                    <c:out value="${pm.label}"/><c:if test="${pm.name() == 'VNPAY'}"> (chưa hỗ trợ)</c:if>
                </label>
            </div>
        </c:forEach>

        <div style="margin-top:16px;">
            <input type="checkbox" name="acceptPolicy" id="acceptPolicy" ${not empty param.acceptPolicy ? 'checked' : ''}>
            <label for="acceptPolicy" style="display:inline;font-weight:normal;">
                Tôi đã đọc và đồng ý với chính sách miễn đổi trả (cửa hàng chỉ kinh doanh xe đã qua sử dụng;
                chỉ bảo hành khi lỗi do nhà sản xuất).
            </label>
        </div>

        <input type="submit" value="Đặt hàng">
    </form>
</body>
</html>
