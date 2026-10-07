<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Thông báo</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<h1>Thông báo</h1>

<c:if test="${not empty flashMessage}">
    <p class="msg-ok"><c:out value="${flashMessage}"/></p>
</c:if>
<c:if test="${not empty flashError}">
    <p class="msg-error"><c:out value="${flashError}"/></p>
</c:if>

<c:choose>
    <c:when test="${empty notifications}">
        <p>Không có thông báo mới.</p>
    </c:when>
    <c:otherwise>
        <p>
        Bạn có <strong><c:out value="${unreadCount}"/></strong> thông báo chưa đọc.
        <c:if test="${unreadCount > 0}">
            <form method="post" action="${pageContext.request.contextPath}/notifications/read" style="display:inline">
                <button type="submit">Đánh dấu tất cả đã đọc</button>
            </form>
        </c:if>
        </p>
        <table>
            <tr>
                <th>Thời gian</th>
                <th>Loại</th>
                <th>Nội dung</th>
                <th>Trạng thái</th>
            </tr>
            <c:forEach var="n" items="${notifications}">
                <tr>
                    <td><c:out value="${n.createdAtDisplay}"/></td>
                    <td><c:out value="${n.type.label}"/></td>
                    <td><c:out value="${n.content}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${n.isRead}">Đã đọc</c:when>
                            <c:otherwise>
                                <strong>Chưa đọc</strong>
                                <form method="post" action="${pageContext.request.contextPath}/notifications/read" style="display:inline">
                                    <input type="hidden" name="id" value="<c:out value='${n.notificationId}'/>">
                                    <button type="submit">Đánh dấu đã đọc</button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </c:otherwise>
</c:choose>
</body>
</html>