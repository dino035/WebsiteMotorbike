<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title><c:out value="${product.name}"/></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<h1><c:out value="${product.name}"/></h1>

<table>
    <c:if test="${product.status == 'AVAILABLE'}">
        <c:choose>
            <c:when test="${not empty sessionScope.currentUser}">
                <form method="post" action="${pageContext.request.contextPath}/cart/add">
                    <input type="hidden" name="productId" value="<c:out value='${product.productId}'/>">
                    <c:if test="${product.typeCode == 'SparePart'}">
                        <label>Số lượng:
                            <input type="number" name="quantity" value="1" min="1" max="99" required>
                        </label>
                    </c:if>
                    <button type="submit">Thêm vào giỏ</button>
                </form>
            </c:when>
            <c:otherwise>
                <p><a href="${pageContext.request.contextPath}/login">Đăng nhập để mua hàng</a></p>
            </c:otherwise>
        </c:choose>
    </c:if>

    <tr><th>Loại</th><td><c:out value="${product.productType}"/></td></tr>
    <tr><th>Giá</th><td><c:out value="${product.price}"/></td></tr>
    <tr><th>Tình trạng</th><td><c:out value="${product.status.label}"/></td></tr>

    <c:if test="${product.typeCode == 'Motorbike'}">
        <tr><th>Hãng</th><td><c:out value="${product.brand}"/></td></tr>
        <tr><th>Dòng xe</th><td><c:out value="${product.model}"/></td></tr>
        <tr><th>Năm đăng ký</th><td><c:out value="${product.registrationYear}"/></td></tr>
        <tr><th>Số chủ sở hữu trước</th><td><c:out value="${product.ownerCount}"/></td></tr>
        <tr><th>Số km đã đi</th><td><c:out value="${product.odometerAtIntake}"/></td></tr>
        <tr><th>Lịch sử tai nạn</th><td><c:out value="${product.accidentHistory}"/></td></tr>
    </c:if>

    <c:if test="${product.typeCode == 'SparePart'}">
        <tr><th>Loại phụ tùng</th><td><c:out value="${product.partType}"/></td></tr>
        <tr><th>Nguồn gốc</th><td><c:out value="${product.partOrigin.label}"/></td></tr>    </c:if>
</table>

<p><a href="${pageContext.request.contextPath}/products">← Quay lại danh sách</a></p>
</body>
</html>