<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Bảo hành của tôi</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Bảo hành của tôi</h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>
    <c:if test="${not empty flashError}">
        <p class="msg-error"><c:out value="${flashError}"/></p>
    </c:if>

    <h2>Sản phẩm có thể gửi yêu cầu bảo hành</h2>
    <p>Bảo hành xe trong 2 năm, phụ tùng trong 6 tháng kể từ ngày mua. Chỉ áp dụng cho sản phẩm trong đơn hàng đã hoàn thành.</p>
    <c:choose>
        <c:when test="${empty options}">
            <p>Bạn chưa có sản phẩm nào trong đơn hàng đã hoàn thành.</p>
        </c:when>
        <c:otherwise>
            <table>
                <tr>
                    <th>Sản phẩm</th>
                    <th>Đơn hàng</th>
                    <th>Ngày mua</th>
                    <th>Bảo hành đến</th>
                    <th>Thao tác</th>
                </tr>
                <c:forEach var="o" items="${options}">
                    <tr>
                        <td><c:out value="${o.productName}"/></td>
                        <td>#<c:out value="${o.orderCode}"/></td>
                        <td><c:out value="${o.purchasedText}"/></td>
                        <td><c:out value="${o.deadlineText}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${o.expired}">Đã hết thời hạn bảo hành</c:when>
                                <c:when test="${o.pending}">Đang chờ xử lý</c:when>
                                <c:otherwise>
                                    <a href="${pageContext.request.contextPath}/warranty/new?itemId=<c:out value='${o.orderItemId}'/>">Gửi yêu cầu bảo hành</a>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>

    <h2>Yêu cầu đã gửi</h2>
    <c:choose>
        <c:when test="${empty requests}">
            <p>Bạn chưa gửi yêu cầu bảo hành nào.</p>
        </c:when>
        <c:otherwise>
            <table>
                <tr>
                    <th>Mã</th>
                    <th>Ngày gửi</th>
                    <th>Sản phẩm</th>
                    <th>Mô tả lỗi</th>
                    <th>Trạng thái</th>
                    <th>Phản hồi của cửa hàng</th>
                </tr>
                <c:forEach var="w" items="${requests}">
                    <tr>
                        <td>#<c:out value="${w.shortCode}"/></td>
                        <td><c:out value="${w.createdAtText}"/></td>
                        <td><c:out value="${w.orderItem.productName}"/></td>
                        <td><c:out value="${w.description}"/></td>
                        <td><c:out value="${w.status.label}"/></td>
                        <td><c:out value="${w.adminNote}"/></td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>
</body>
</html>
