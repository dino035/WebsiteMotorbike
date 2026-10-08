<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Lịch lắp đặt (Admin)</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Lịch lắp đặt tại cửa hàng</h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>
    <c:if test="${not empty flashError}">
        <p class="msg-error"><c:out value="${flashError}"/></p>
    </c:if>

    <p>Tối đa ${maxPerDay} lượt mỗi ngày (không tính lịch đã hủy).</p>

    <form action="${pageContext.request.contextPath}/admin/installations" method="get">
        <label>Lọc theo trạng thái:</label>
        <select name="status">
            <option value="">Tất cả</option>
            <c:forEach var="s" items="${statuses}">
                <option value="${s}" ${selectedStatus == s.name() ? 'selected' : ''}><c:out value="${s.label}"/></option>
            </c:forEach>
        </select>
        <input type="submit" value="Lọc">
    </form>

    <c:choose>
        <c:when test="${empty installations}">
            <p>Không có lịch lắp đặt nào.</p>
        </c:when>
        <c:otherwise>
            <table>
                <tr>
                    <th>Ngày hẹn</th>
                    <th>Đơn hàng</th>
                    <th>Người nhận</th>
                    <th class="num">Phí công</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                </tr>
                <c:forEach var="r" items="${installations}">
                    <tr>
                        <td><c:out value="${r.scheduledDateText}"/></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/admin/orders/detail?id=<c:out value='${r.order.orderId}'/>">
                                #<c:out value="${r.order.shortCode}"/>
                            </a>
                        </td>
                        <td><c:out value="${r.order.receiverName}"/> - <c:out value="${r.order.receiverPhone}"/></td>
                        <td class="num"><fmt:formatNumber value="${r.laborFee}" type="number" groupingUsed="true"/></td>
                        <td><c:out value="${r.status.label}"/></td>
                        <td>
                            <c:if test="${r.status.name() == 'SCHEDULED'}">
                                <form class="inline-form" action="${pageContext.request.contextPath}/admin/installations/update" method="post">
                                    <input type="hidden" name="id" value="<c:out value='${r.installationId}'/>">
                                    <input type="hidden" name="action" value="RESCHEDULE">
                                    <input type="date" name="date" min="${minDate}" max="${maxDate}"
                                           value="<c:out value='${r.scheduledDateIso}'/>" style="width:auto;">
                                    <input type="submit" value="Đổi ngày">
                                </form>
                                <form class="inline-form" action="${pageContext.request.contextPath}/admin/installations/update" method="post">
                                    <input type="hidden" name="id" value="<c:out value='${r.installationId}'/>">
                                    <input type="hidden" name="action" value="DONE">
                                    <input type="submit" value="Đã lắp đặt">
                                </form>
                                <form class="inline-form" action="${pageContext.request.contextPath}/admin/installations/update" method="post">
                                    <input type="hidden" name="id" value="<c:out value='${r.installationId}'/>">
                                    <input type="hidden" name="action" value="CANCEL">
                                    <input type="submit" value="Hủy lịch" onclick="return confirm('Hủy lịch lắp đặt này?')">
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>
</body>
</html>
