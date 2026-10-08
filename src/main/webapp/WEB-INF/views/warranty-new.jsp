<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Gửi yêu cầu bảo hành</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Gửi yêu cầu bảo hành</h1>

    <c:if test="${not empty error}">
        <p class="msg-error"><c:out value="${error}"/></p>
    </c:if>

    <p>Sản phẩm: <strong><c:out value="${item.productName}"/></strong>
       (đơn hàng #<c:out value="${item.order.shortCode}"/>)</p>

    <form action="${pageContext.request.contextPath}/warranty/new" method="post">
        <input type="hidden" name="itemId" value="<c:out value='${item.orderItemId}'/>">

        <label>Mô tả lỗi:</label>
        <textarea name="description" rows="5" required maxlength="1000" style="width:100%;box-sizing:border-box;"><c:out value="${param.description}"/></textarea>

        <label>Đường dẫn ảnh hoặc video minh chứng (không bắt buộc):</label>
        <input type="text" name="evidenceUrl" maxlength="500" placeholder="https://..."
               value="<c:out value='${param.evidenceUrl}'/>">

        <input type="submit" value="Gửi yêu cầu">
    </form>

    <p><a href="${pageContext.request.contextPath}/warranty">← Quay lại Bảo hành của tôi</a></p>
</body>
</html>
