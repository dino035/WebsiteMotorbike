<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Danh sách sản phẩm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Danh sách sản phẩm</h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok" style="color: #15803d;"><c:out value="${flashMessage}"/></p>
    </c:if>

    <c:if test="${not empty flashError}">
        <p class="msg-error" style="color: #dc2626;"><c:out value="${flashError}"/></p>
    </c:if>

    <form action="${pageContext.request.contextPath}/products" method="get">
        <input type="text" name="keyword" placeholder="Tìm theo tên..."
               value="<c:out value='${param.keyword}'/>">

        <select name="type">
            <option value="">Tất cả loại</option>
            <option value="Motorbike" ${param.type == 'Motorbike' ? 'selected' : ''}>Xe máy</option>
            <option value="SparePart" ${param.type == 'SparePart' ? 'selected' : ''}>Phụ tùng</option>
        </select>

        <select name="status">
            <option value="">Tất cả tình trạng</option>
            <option value="AVAILABLE" ${param.status == 'AVAILABLE' ? 'selected' : ''}>Có sẵn</option>
            <option value="SOLD" ${param.status == 'SOLD' ? 'selected' : ''}>Hết hàng</option>
        </select>

        <input type="number" name="minPrice" min="0" placeholder="Giá từ"
               value="<c:out value='${param.minPrice}'/>">
        <input type="number" name="maxPrice" min="0" placeholder="Giá đến"
               value="<c:out value='${param.maxPrice}'/>">

        <button type="submit">Tìm kiếm</button>
        <a href="${pageContext.request.contextPath}/products">Xóa bộ lọc</a>
</form>

<c:set var="isAdmin" value="${sessionScope.currentUser.userRole == 'ADMIN'}"/>
<table>
    <tr>
        <th>Tên</th>
        <th>Giá</th>
        <th>Loại</th>
        <th>Tình trạng</th>
        <th>Chi tiết</th>
        <c:if test="${isAdmin}">
            <th>Xóa</th>
            <th>Chỉnh sửa</th>
        </c:if>
    </tr>
    <c:forEach var="product" items="${products}">
        <tr>
            <td>
                <a href="${pageContext.request.contextPath}/products/detail?id=${product.productId}">
                    <c:out value="${product.name}"/>
                </a>
            </td>
            <td><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/></td>
            <td><c:out value="${product.productType}"/></td>
            <td><c:out value="${product.status.label}"/></td>
            <td><c:out value="${product.detail}"/></td>
            <c:if test="${isAdmin}">
                <td>
                    <form method="post" action="${pageContext.request.contextPath}/products/delete"
                          onsubmit="return confirm('Bạn có chắc chắn muốn xóa sản phẩm này?')">
                        <input type="hidden" name="id" value="<c:out value='${product.productId}'/>">
                        <button type="submit" class="btn-danger">Xóa</button>
                    </form>
                </td>
                <td>
                    <a href="${pageContext.request.contextPath}/products/edit?id=${product.productId}">Chỉnh sửa</a>
                </td>
            </c:if>
        </tr>
    </c:forEach>
</table>

    <c:if test="${empty products}">
        <p>Không tìm thấy sản phẩm phù hợp.</p>
    </c:if>
</body>
</html>