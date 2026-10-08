<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Sổ địa chỉ</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>Sổ địa chỉ</h1>

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
        <c:when test="${empty addresses}">
            <p>Bạn chưa lưu địa chỉ nào.</p>
        </c:when>
        <c:otherwise>
            <table>
                <tr>
                    <th>Người nhận</th>
                    <th>Số điện thoại</th>
                    <th>Địa chỉ</th>
                    <th>Mặc định</th>
                    <th>Thao tác</th>
                </tr>
                <c:forEach var="a" items="${addresses}">
                    <tr>
                        <td><c:out value="${a.receiverName}"/></td>
                        <td><c:out value="${a.receiverPhone}"/></td>
                        <td><c:out value="${a.addressLine}"/></td>
                        <td><c:if test="${a.defaultAddress}"><strong>Mặc định</strong></c:if></td>
                        <td>
                            <a href="${pageContext.request.contextPath}/addresses?edit=<c:out value='${a.addressId}'/>">Sửa</a>
                            <c:if test="${not a.defaultAddress}">
                                <form class="inline-form" action="${pageContext.request.contextPath}/addresses/action" method="post">
                                    <input type="hidden" name="id" value="<c:out value='${a.addressId}'/>">
                                    <input type="hidden" name="action" value="DEFAULT">
                                    <input type="submit" value="Đặt mặc định">
                                </form>
                            </c:if>
                            <form class="inline-form" action="${pageContext.request.contextPath}/addresses/action" method="post">
                                <input type="hidden" name="id" value="<c:out value='${a.addressId}'/>">
                                <input type="hidden" name="action" value="DELETE">
                                <input type="submit" value="Xóa" onclick="return confirm('Xóa địa chỉ này?')">
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>

    <%-- Khi có lỗi: giữ lại dữ liệu vừa nhập (param); mở để sửa: lấy từ địa chỉ đang sửa; còn lại: biểu mẫu trống --%>
    <c:set var="submitted" value="${not empty param.receiverName or not empty param.receiverPhone or not empty param.addressLine}"/>
    <h2><c:choose><c:when test="${not empty editing}">Sửa địa chỉ</c:when><c:otherwise>Thêm địa chỉ mới</c:otherwise></c:choose></h2>
    <p>Mỗi tài khoản lưu tối đa ${maxAddresses} địa chỉ.</p>

    <form action="${pageContext.request.contextPath}/addresses/save" method="post">
        <c:if test="${not empty editing}">
            <input type="hidden" name="addressId" value="<c:out value='${editing.addressId}'/>">
        </c:if>

        <label>Họ tên người nhận:</label>
        <input type="text" name="receiverName" required maxlength="100"
               value="<c:out value='${submitted ? param.receiverName : editing.receiverName}'/>">

        <label>Số điện thoại:</label>
        <input type="text" name="receiverPhone" required
               value="<c:out value='${submitted ? param.receiverPhone : editing.receiverPhone}'/>">

        <label>Địa chỉ:</label>
        <textarea name="addressLine" rows="3" required maxlength="500" style="width:100%;box-sizing:border-box;"><c:out value="${submitted ? param.addressLine : editing.addressLine}"/></textarea>

        <div style="margin-top:12px;">
            <input type="checkbox" name="makeDefault" id="makeDefault"
                   ${submitted ? (not empty param.makeDefault ? 'checked' : '') : (editing.defaultAddress ? 'checked' : '')}>
            <label for="makeDefault" style="display:inline;font-weight:normal;">Đặt làm địa chỉ mặc định</label>
        </div>

        <input type="submit" value="${not empty editing ? 'Lưu thay đổi' : 'Thêm địa chỉ'}">
        <c:if test="${not empty editing}">
            <a href="${pageContext.request.contextPath}/addresses">Hủy sửa</a>
        </c:if>
    </form>
</body>
</html>
