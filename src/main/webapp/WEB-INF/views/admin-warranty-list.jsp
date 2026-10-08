<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Quản lý bảo hành</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Quản lý bảo hành</h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>
    <c:if test="${not empty flashError}">
        <p class="msg-error"><c:out value="${flashError}"/></p>
    </c:if>

    <form action="${pageContext.request.contextPath}/admin/warranty" method="get">
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
        <c:when test="${empty requests}">
            <p>Không có yêu cầu bảo hành nào.</p>
        </c:when>
        <c:otherwise>
            <table>
                <tr>
                    <th>Mã</th>
                    <th>Ngày gửi</th>
                    <th>Sản phẩm</th>
                    <th>Mô tả lỗi</th>
                    <th>Trạng thái</th>
                </tr>
                <c:forEach var="w" items="${requests}">
                    <tr>
                        <td>
                            <a href="${pageContext.request.contextPath}/admin/warranty/detail?id=<c:out value='${w.warrantyId}'/>">
                                #<c:out value="${w.shortCode}"/>
                            </a>
                        </td>
                        <td><c:out value="${w.createdAtText}"/></td>
                        <td><c:out value="${w.orderItem.productName}"/></td>
                        <td><c:out value="${w.description}"/></td>
                        <td><c:out value="${w.status.label}"/></td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>
</body>
</html>
