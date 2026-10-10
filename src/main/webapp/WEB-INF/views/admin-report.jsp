<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Báo cáo doanh thu</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Báo cáo doanh thu</h1>

    <c:if test="${not empty error}">
        <p class="msg-error"><c:out value="${error}"/></p>
    </c:if>

    <form action="${pageContext.request.contextPath}/admin/reports" method="get">
        <label>Từ ngày:</label>
        <input type="date" name="from" value="<c:out value='${from}'/>" max="<c:out value='${today}'/>" required>
        <label>Đến ngày:</label>
        <input type="date" name="to" value="<c:out value='${to}'/>" max="<c:out value='${today}'/>" required>
        <input type="submit" value="Xem báo cáo">
    </form>

    <h2>Doanh thu</h2>
    <c:choose>
        <c:when test="${noData}">
            <p>Không có dữ liệu trong khoảng thời gian này.</p>
        </c:when>
        <c:otherwise>
            <p class="msg-notice">Doanh thu tính trên các đơn hàng đã hoàn thành (đã thu tiền), theo ngày đặt.</p>
            <table>
                <tr><th>Tổng doanh thu</th><td class="num"><fmt:formatNumber value="${totalRevenue}" type="number" groupingUsed="true"/></td></tr>
                <tr><th>Số đơn hoàn thành</th><td class="num"><c:out value="${completedOrders}"/></td></tr>
                <tr><th>Giá trị trung bình mỗi đơn</th><td class="num"><fmt:formatNumber value="${averageOrder}" type="number" groupingUsed="true"/></td></tr>
            </table>

            <h2>Doanh thu theo ngày</h2>
            <c:choose>
                <c:when test="${empty days}">
                    <p>Chưa có đơn hàng nào hoàn thành trong khoảng thời gian này.</p>
                </c:when>
                <c:otherwise>
                    <table>
                        <tr><th>Ngày</th><th>Số đơn</th><th class="num">Doanh thu</th></tr>
                        <c:forEach var="d" items="${days}">
                            <tr>
                                <td><c:out value="${d.dateText}"/></td>
                                <td><c:out value="${d.orderCount}"/></td>
                                <td class="num"><fmt:formatNumber value="${d.revenue}" type="number" groupingUsed="true"/></td>
                            </tr>
                        </c:forEach>
                    </table>
                </c:otherwise>
            </c:choose>

            <h2>Đơn hàng theo trạng thái</h2>
            <table>
                <tr><th>Trạng thái</th><th>Số đơn</th></tr>
                <c:forEach var="entry" items="${statusCounts}">
                    <tr>
                        <td><c:out value="${entry.key.label}"/></td>
                        <td><c:out value="${entry.value}"/></td>
                    </tr>
                </c:forEach>
            </table>

            <h2>Sản phẩm bán chạy (tối đa <c:out value="${topN}"/>)</h2>
            <c:choose>
                <c:when test="${empty bestSellers}">
                    <p>Chưa có sản phẩm nào được bán (đơn hoàn thành) trong khoảng thời gian này.</p>
                </c:when>
                <c:otherwise>
                    <table>
                        <tr><th>#</th><th>Sản phẩm</th><th>Số lượng bán</th><th class="num">Doanh thu</th></tr>
                        <c:forEach var="b" items="${bestSellers}" varStatus="loop">
                            <tr>
                                <td><c:out value="${loop.index + 1}"/></td>
                                <td><c:out value="${b.productName}"/></td>
                                <td><c:out value="${b.quantity}"/></td>
                                <td class="num"><fmt:formatNumber value="${b.revenue}" type="number" groupingUsed="true"/></td>
                            </tr>
                        </c:forEach>
                    </table>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>

    <h2>Tồn kho hiện tại</h2>
    <p class="msg-notice">Tồn kho là số liệu tại thời điểm xem, không phụ thuộc khoảng thời gian đã chọn.</p>
    <table>
        <tr><th>Loại</th><th>Còn bán</th><th>Hết hàng</th><th class="num">Giá trị hàng còn bán</th></tr>
        <c:forEach var="row" items="${stock}">
            <tr>
                <td><c:out value="${row.label}"/></td>
                <td><c:out value="${row.available}"/></td>
                <td><c:out value="${row.sold}"/></td>
                <td class="num"><fmt:formatNumber value="${row.availableValue}" type="number" groupingUsed="true"/></td>
            </tr>
        </c:forEach>
    </table>

    <p><a href="${pageContext.request.contextPath}/admin/orders">← Quản lý đơn hàng</a></p>
</body>
</html>
