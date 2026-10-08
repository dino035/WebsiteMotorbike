<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Quản lý tài khoản</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Quản lý tài khoản khách hàng</h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>
    <c:if test="${not empty flashError}">
        <p class="msg-error"><c:out value="${flashError}"/></p>
    </c:if>

    <form action="${pageContext.request.contextPath}/admin/accounts" method="get">
        <label>Tìm theo email:</label>
        <input type="text" name="q" value="<c:out value='${param.q}'/>">

        <label>Trạng thái:</label>
        <select name="status">
            <option value="ALL" ${selectedStatus == 'ALL' ? 'selected' : ''}>Tất cả</option>
            <option value="ACTIVE" ${selectedStatus == 'ACTIVE' ? 'selected' : ''}>Đang hoạt động</option>
            <option value="LOCKED" ${selectedStatus == 'LOCKED' ? 'selected' : ''}>Đã khóa</option>
        </select>
        <input type="submit" value="Lọc">
    </form>

    <c:choose>
        <c:when test="${empty rows}">
            <p>Không có tài khoản nào.</p>
        </c:when>
        <c:otherwise>
            <table>
                <tr>
                    <th>Email</th>
                    <th>Số điện thoại</th>
                    <th>Trạng thái</th>
                    <th>Đơn chưa hoàn tất</th>
                    <th>Thao tác</th>
                </tr>
                <c:forEach var="row" items="${rows}">
                    <tr>
                        <td><c:out value="${row.user.email}"/></td>
                        <td><c:out value="${row.user.phone}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${row.locked}">
                                    <span class="msg-error">Đã khóa</span>
                                    (<c:out value="${row.lock.lockedAtText}"/>)
                                    <c:if test="${not empty row.lock.reason}"><br>Lý do: <c:out value="${row.lock.reason}"/></c:if>
                                </c:when>
                                <c:otherwise>Đang hoạt động</c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${row.unfinishedOrders}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${row.locked}">
                                    <form class="inline-form" action="${pageContext.request.contextPath}/admin/accounts/unlock" method="post">
                                        <input type="hidden" name="id" value="<c:out value='${row.user.accountId}'/>">
                                        <input type="submit" value="Mở khóa">
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <form class="inline-form" action="${pageContext.request.contextPath}/admin/accounts/lock" method="post">
                                        <input type="hidden" name="id" value="<c:out value='${row.user.accountId}'/>">
                                        <input type="text" name="reason" maxlength="200" placeholder="Lý do (không bắt buộc)" style="width:180px;">
                                        <c:if test="${row.unfinishedOrders > 0}">
                                            <br><span class="msg-notice">Cảnh báo: tài khoản có <c:out value="${row.unfinishedOrders}"/> đơn hàng chưa hoàn tất.</span>
                                            <br><label style="display:inline;font-weight:normal;"><input type="checkbox" name="force"> Tôi hiểu, vẫn khóa</label>
                                        </c:if>
                                        <input type="submit" value="Khóa" onclick="return confirm('Khóa tài khoản này?')">
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
