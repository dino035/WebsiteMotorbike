<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title><c:out value="${product.name}"/></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<h1><c:out value="${product.name}"/></h1>

<c:if test="${not empty photos}">
    <div class="photo-gallery">
        <c:forEach var="photo" items="${photos}">
            <a href="<c:out value='${photo.imageUrl}'/>" target="_blank">
                <img src="<c:out value='${photo.imageUrl}'/>"
                     alt="<c:out value='${product.name}'/>">
            </a>
        </c:forEach>
    </div>
</c:if>

<table>
    <tr><th>Loại</th><td><c:out value="${product.productType}"/></td></tr>
    <tr><th>Giá</th><td><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/></td></tr>
    <tr><th>Tình trạng</th><td><c:out value="${product.status.label}"/></td></tr>

    <c:if test="${product.typeCode == 'Motorbike'}">
        <tr><th>Hãng</th><td><c:out value="${product.brand}"/></td></tr>
        <tr><th>Dòng xe</th><td><c:out value="${product.model}"/></td></tr>
        <tr><th>Năm đăng ký</th><td><c:out value="${product.registrationYear}"/></td></tr>
        <tr><th>Số chủ sở hữu trước</th><td><c:out value="${product.ownerCount}"/></td></tr>
        <tr><th>Số km đã đi</th><td><c:out value="${product.odometerAtIntake}"/></td></tr>
        <tr><th>Lịch sử tai nạn</th><td><c:out value="${product.accidentHistory}"/></td></tr>
    </c:if>

    <c:if test="${product.typeCode == 'SparePart'}">
        <tr><th>Loại phụ tùng</th><td><c:out value="${product.partType}"/></td></tr>
        <tr><th>Nguồn gốc</th><td><c:out value="${product.partOrigin.label}"/></td></tr>
    </c:if>
</table>

<c:if test="${product.typeCode == 'Motorbike'}">
    <h2>Đánh giá tình trạng xe</h2>
    <c:choose>
        <c:when test="${not empty product.conditionAssessment}">
            <c:set var="assessment" value="${product.conditionAssessment}"/>
            <table>
                <tr><th>Động cơ</th><td><c:out value="${assessment.engineRating.label}"/></td></tr>
                <tr><th>Phanh</th><td><c:out value="${assessment.brakeRating.label}"/></td></tr>
                <tr><th>Lốp</th><td><c:out value="${assessment.tireRating.label}"/></td></tr>
                <tr><th>Xích (sên)</th><td><c:out value="${assessment.chainRating.label}"/></td></tr>
                <tr><th>Hệ thống điện</th><td><c:out value="${assessment.electricalRating.label}"/></td></tr>
                <tr><th>Điểm tình trạng</th>
                    <td><fmt:formatNumber value="${assessment.adjustmentCoefficient}" minFractionDigits="1" maxFractionDigits="1"/>/10</td></tr>
            </table>
            <p><strong><c:out value="${assessment.verdict}"/></strong></p>
            <c:if test="${not empty assessment.repairAdvice}">
                <p>Hạng mục cần lưu ý:</p>
                <ul>
                    <c:forEach var="tip" items="${assessment.repairAdvice}">
                        <li><c:out value="${tip}"/></li>
                    </c:forEach>
                </ul>
            </c:if>
        </c:when>
        <c:otherwise>
            <p>Chưa có đánh giá tình trạng.</p>
        </c:otherwise>
    </c:choose>
</c:if>

<%-- Thêm vào giỏ: chỉ khi còn hàng --%>
<c:if test="${product.status == 'AVAILABLE'}">
    <c:choose>
        <c:when test="${not empty sessionScope.currentUser}">
            <form method="post" action="${pageContext.request.contextPath}/cart/add">
                <input type="hidden" name="productId" value="<c:out value='${product.productId}'/>">
                <c:if test="${product.typeCode == 'SparePart'}">
                    <label>Số lượng:
                        <input type="number" name="quantity" value="1" min="1" max="99" required>
                    </label>
                </c:if>
                <button type="submit">Thêm vào giỏ</button>
            </form>
        </c:when>
        <c:otherwise>
            <p><a href="${pageContext.request.contextPath}/login">Đăng nhập để mua hàng</a></p>
        </c:otherwise>
    </c:choose>
</c:if>

<%-- Đánh giá: công khai cho mọi người xem --%>
<h2>Đánh giá sản phẩm</h2>

<c:if test="${not empty flashMessage}">
    <p class="msg-ok"><c:out value="${flashMessage}"/></p>
</c:if>
<c:if test="${not empty flashError}">
    <p class="msg-error"><c:out value="${flashError}"/></p>
</c:if>

<c:choose>
    <c:when test="${empty reviews}">
        <p>Chưa có đánh giá nào.</p>
    </c:when>
    <c:otherwise>
        <p>
            Điểm trung bình:
            <strong><fmt:formatNumber value="${averageRating}" maxFractionDigits="1"/></strong>/5
            (<c:out value="${reviews.size()}"/> đánh giá)
        </p>
        <c:forEach var="r" items="${reviews}">
            <div>
                <strong>Khách hàng</strong> - <c:out value="${r.rating}"/>/5
                <small>(<c:out value="${r.createdAtDisplay}"/>)</small>
                <p><c:out value="${r.comment}"/></p>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${empty sessionScope.currentUser}">
        <p><a href="${pageContext.request.contextPath}/login">Đăng nhập để viết đánh giá</a></p>
    </c:when>
    <c:when test="${alreadyReviewed}">
        <p>Bạn đã đánh giá sản phẩm này.</p>
    </c:when>
    <c:when test="${canReview}">
        <form method="post" action="${pageContext.request.contextPath}/reviews/add">
            <input type="hidden" name="productId" value="<c:out value='${product.productId}'/>">
            <label>Điểm:
                <select name="rating" required>
                    <option value="5">5 - Rất tốt</option>
                    <option value="4">4 - Tốt</option>
                    <option value="3">3 - Bình thường</option>
                    <option value="2">2 - Kém</option>
                    <option value="1">1 - Rất kém</option>
                </select>
            </label>
            <br>
            <textarea name="comment" rows="4" cols="50" maxlength="1000" required
                      placeholder="Nhận xét của bạn về sản phẩm"></textarea>
            <br>
            <button type="submit">Gửi đánh giá</button>
        </form>
    </c:when>
    <c:otherwise>
        <p>Bạn chỉ có thể đánh giá sản phẩm đã mua.</p>
    </c:otherwise>
</c:choose>

<p><a href="${pageContext.request.contextPath}/products">← Quay lại danh sách</a></p>
</body>
</html>