<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Danh sách sản phẩm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <h1>Danh sách sản phẩm</h1>
    <table>
        <tr>
            <th>Tên</th>
            <th>Giá</th>
            <th>Loại</th>
            <th>Tình trạng</th>
            <th>Chi tiết</th>
            <th>Xóa</th>
            <th>Chỉnh sửa</th>
        </tr>
        <c:forEach var="product" items="${products}">
            <tr>
                <td>${product.name}</td>
                <td>${product.price}</td>
                <td>${product.productType}</td>
                <td>${product.status.label}</td>
                <td>${product.detail}</td>
                <td>
                    <a href="${pageContext.request.contextPath}/products/delete?id=${product.productId}" onclick="return confirm('Bạn có chắc chắn muốn xóa sản phẩm này?')">Xóa</a>
                </td>
                <td>
                    <a href="${pageContext.request.contextPath}/products/edit?id=${product.productId}">Chỉnh sửa</a>
                </td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>