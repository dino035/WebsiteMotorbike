<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Xe của tôi</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Xe của tôi</h1>

    <c:if test="${not empty flashMessage}">
        <p class="msg-ok"><c:out value="${flashMessage}"/></p>
    </c:if>
    <c:if test="${not empty flashError}">
        <p class="msg-error"><c:out value="${flashError}"/></p>
    </c:if>
    <c:if test="${not empty error}">
        <p class="msg-error"><c:out value="${error}"/></p>
    </c:if>

    <c:choose>
        <c:when test="${empty profiles}">
            <p>Bạn chưa khai báo xe nào.</p>
        </c:when>
        <c:otherwise>
            <table>
                <tr>
                    <th>Biệt danh</th>
                    <th>Hãng</th>
                    <th>Dòng xe</th>
                    <th>Thao tác</th>
                </tr>
                <c:forEach var="p" items="${profiles}">
                    <tr>
                        <td><c:out value="${p.nickname}"/></td>
                        <td><c:out value="${p.brand}"/></td>
                        <td><c:out value="${p.model}"/></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/vehicles?edit=<c:out value='${p.profileId}'/>">Sửa</a>
                            <form class="inline-form" action="${pageContext.request.contextPath}/vehicles/delete" method="post">
                                <input type="hidden" name="id" value="<c:out value='${p.profileId}'/>">
                                <input type="submit" class="btn-danger" value="Xóa" onclick="return confirm('Xóa xe này?')">
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>

    <%-- Khi có lỗi: giữ lại dữ liệu vừa nhập (param); mở để sửa: lấy từ hồ sơ đang sửa; còn lại: biểu mẫu trống --%>
    <c:set var="submitted" value="${not empty param.nickname or not empty param.vehicle}"/>
    <c:set var="selectedVehicle" value="${submitted ? param.vehicle : (not empty editing ? editing.brand.concat('|').concat(editing.model) : '')}"/>
    <h2><c:choose><c:when test="${not empty editing}">Sửa xe</c:when><c:otherwise>Thêm xe của tôi</c:otherwise></c:choose></h2>
    <p>Mỗi tài khoản khai báo tối đa ${maxProfiles} xe. Thông tin này giúp kiểm tra phụ tùng có hợp với xe của bạn không.</p>

    <form action="${pageContext.request.contextPath}/vehicles/save" method="post">
        <c:if test="${not empty editing}">
            <input type="hidden" name="profileId" value="<c:out value='${editing.profileId}'/>">
        </c:if>

        <label>Biệt danh (ví dụ: Xe đi làm):</label>
        <input type="text" name="nickname" required maxlength="50"
               value="<c:out value='${submitted ? param.nickname : editing.nickname}'/>">

        <label>Hãng và dòng xe:</label>
        <select name="vehicle">
            <option value="">-- Chọn dòng xe --</option>
            <c:forEach var="entry" items="${options}">
                <optgroup label="<c:out value='${entry.key}'/>">
                    <c:forEach var="model" items="${entry.value}">
                        <c:set var="optionValue" value="${entry.key}|${model}"/>
                        <option value="<c:out value='${optionValue}'/>" ${selectedVehicle == optionValue ? 'selected' : ''}>
                            <c:out value="${model}"/>
                        </option>
                    </c:forEach>
                </optgroup>
            </c:forEach>
        </select>

        <input type="submit" value="${not empty editing ? 'Lưu thay đổi' : 'Thêm xe'}">
        <c:if test="${not empty editing}">
            <a href="${pageContext.request.contextPath}/vehicles">Hủy sửa</a>
        </c:if>
    </form>
</body>
</html>
