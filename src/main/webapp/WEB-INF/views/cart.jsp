<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Giỏ hàng</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Giỏ hàng</h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>
    <c:if test="${not empty flashError}">
        <p class="msg-error"><c:out value="${flashError}"/></p>
    </c:if>
    <c:forEach var="notice" items="${notices}">
        <p class="msg-notice"><c:out value="${notice}"/></p>
    </c:forEach>

    <c:choose>
        <c:when test="${empty lines}">
            <p>Giỏ hàng của bạn đang trống.</p>
            <p><a href="${pageContext.request.contextPath}/products">Xem danh sách sản phẩm</a></p>
        </c:when>
        <c:otherwise>
            <table>
                <tr>
                    <th>Sản phẩm</th>
                    <th>Loại</th>
                    <th class="num">Đơn giá</th>
                    <th>Số lượng</th>
                    <th class="num">Thành tiền</th>
                    <th>Xóa</th>
                </tr>
                <c:forEach var="line" items="${lines}">
                    <tr>
                        <td>
                            <a href="${pageContext.request.contextPath}/products/detail?id=<c:out value='${line.productId}'/>">
                                <c:out value="${line.name}"/>
                            </a>
                        </td>
                        <td><c:out value="${line.productType}"/></td>
                        <td class="num"><fmt:formatNumber value="${line.unitPrice}" type="number" groupingUsed="true"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${line.motorbike}">1</c:when>
                                <c:otherwise>
                                    <form class="inline-form" action="${pageContext.request.contextPath}/cart/update" method="post">
                                        <input type="hidden" name="cartItemId" value="<c:out value='${line.cartItemId}'/>">
                                        <input type="number" name="quantity" min="1" max="99" value="${line.quantity}">
                                        <input type="submit" value="Cập nhật">
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="num"><fmt:formatNumber value="${line.lineTotal}" type="number" groupingUsed="true"/></td>
                        <td>
                            <form class="inline-form" action="${pageContext.request.contextPath}/cart/remove" method="post">
                                <input type="hidden" name="cartItemId" value="<c:out value='${line.cartItemId}'/>">
                                <input type="submit" value="Xóa" onclick="return confirm('Xóa sản phẩm này khỏi giỏ hàng?')">
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </table>

            <p class="cart-total">Tổng cộng: <fmt:formatNumber value="${total}" type="number" groupingUsed="true"/></p>
            <p>Xe trong giỏ được tạm giữ cho bạn ${holdMinutes} phút.</p>
        </c:otherwise>
    </c:choose>
</body>
</html>
