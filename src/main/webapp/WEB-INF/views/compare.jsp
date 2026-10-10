<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>So sánh xe</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
    <h1>So sánh xe</h1>

    <c:if test="${not empty error}">
        <p class="msg-error"><c:out value="${error}"/></p>
    </c:if>

    <h2>Chọn xe để so sánh</h2>
    <c:choose>
        <c:when test="${empty bikes}">
            <p>Hiện chưa có xe nào đang bán để so sánh.</p>
        </c:when>
        <c:otherwise>
            <p>Chọn từ <c:out value="${minCompare}"/> đến <c:out value="${maxCompare}"/> xe.</p>
            <form id="compareForm" class="compare-picker" action="${pageContext.request.contextPath}/compare" method="get">
                <c:forEach var="bike" items="${bikes}">
                    <label>
                        <input type="checkbox" name="id" value="<c:out value='${bike.productId}'/>"
                               ${selectedIds.contains(bike.productId) ? 'checked' : ''}>
                        <c:out value="${bike.name}"/> -
                        <c:out value="${bike.brand}"/> <c:out value="${bike.model}"/> -
                        <fmt:formatNumber value="${bike.price}" type="number" groupingUsed="true"/>
                    </label>
                </c:forEach>
                <input type="submit" value="So sánh">
            </form>
        </c:otherwise>
    </c:choose>

    <c:if test="${not empty items}">
        <h2>Bảng so sánh</h2>
        <p class="msg-notice">Ô tô nền xanh là giá trị tốt nhất của tiêu chí đó (giá thấp nhất, đời mới nhất, ít chủ nhất, ít km nhất, điểm tình trạng cao nhất); chỉ tô khi các xe khác nhau.</p>
        <div class="compare-wrap">
            <table class="compare">
                <tr>
                    <th class="row-label">Xe</th>
                    <c:forEach var="item" items="${items}">
                        <td>
                            <c:if test="${not empty item.photoUrl}">
                                <img src="<c:out value='${item.photoUrl}'/>" alt="<c:out value='${item.motorbike.name}'/>"><br>
                            </c:if>
                            <a href="${pageContext.request.contextPath}/products/detail?id=<c:out value='${item.motorbike.productId}'/>">
                                <c:out value="${item.motorbike.name}"/>
                            </a>
                        </td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Hãng - dòng xe</th>
                    <c:forEach var="item" items="${items}">
                        <td><c:out value="${item.motorbike.brand}"/> - <c:out value="${item.motorbike.model}"/></td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Giá</th>
                    <c:forEach var="item" items="${items}">
                        <td class="${item.bestPrice ? 'best' : ''}">
                            <fmt:formatNumber value="${item.motorbike.price}" type="number" groupingUsed="true"/>
                        </td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Tình trạng bán</th>
                    <c:forEach var="item" items="${items}">
                        <td><c:out value="${item.motorbike.status.label}"/></td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Năm đăng ký</th>
                    <c:forEach var="item" items="${items}">
                        <td class="${item.bestYear ? 'best' : ''}">
                            <c:out value="${empty item.motorbike.registrationYear ? 'Chưa có' : item.motorbike.registrationYear}"/>
                        </td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Số chủ sở hữu trước</th>
                    <c:forEach var="item" items="${items}">
                        <td class="${item.bestOwners ? 'best' : ''}">
                            <c:out value="${empty item.motorbike.ownerCount ? 'Chưa có' : item.motorbike.ownerCount}"/>
                        </td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Số km khi nhập kho</th>
                    <c:forEach var="item" items="${items}">
                        <td class="${item.bestOdometer ? 'best' : ''}">
                            <c:choose>
                                <c:when test="${empty item.motorbike.odometerAtIntake}">Chưa có</c:when>
                                <c:otherwise><fmt:formatNumber value="${item.motorbike.odometerAtIntake}" type="number" groupingUsed="true"/></c:otherwise>
                            </c:choose>
                        </td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Lịch sử tai nạn</th>
                    <c:forEach var="item" items="${items}">
                        <td><c:out value="${empty item.motorbike.accidentHistory ? 'Chưa có' : item.motorbike.accidentHistory}"/></td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Đánh giá tình trạng</th>
                    <c:forEach var="item" items="${items}">
                        <c:set var="assessment" value="${item.motorbike.conditionAssessment}"/>
                        <td class="${item.bestScore ? 'best' : ''}">
                            <c:choose>
                                <c:when test="${empty assessment}">Chưa có đánh giá</c:when>
                                <c:otherwise>
                                    <fmt:formatNumber value="${assessment.adjustmentCoefficient}" minFractionDigits="1" maxFractionDigits="1"/>/10<br>
                                    <c:out value="${assessment.verdict}"/>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Động cơ</th>
                    <c:forEach var="item" items="${items}">
                        <td><c:out value="${empty item.motorbike.conditionAssessment ? '-' : item.motorbike.conditionAssessment.engineRating.label}"/></td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Phanh</th>
                    <c:forEach var="item" items="${items}">
                        <td><c:out value="${empty item.motorbike.conditionAssessment ? '-' : item.motorbike.conditionAssessment.brakeRating.label}"/></td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Lốp</th>
                    <c:forEach var="item" items="${items}">
                        <td><c:out value="${empty item.motorbike.conditionAssessment ? '-' : item.motorbike.conditionAssessment.tireRating.label}"/></td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Truyền động (xích/dây curoa)</th>
                    <c:forEach var="item" items="${items}">
                        <td><c:out value="${empty item.motorbike.conditionAssessment ? '-' : item.motorbike.conditionAssessment.chainRating.label}"/></td>
                    </c:forEach>
                </tr>
                <tr>
                    <th class="row-label">Hệ thống điện</th>
                    <c:forEach var="item" items="${items}">
                        <td><c:out value="${empty item.motorbike.conditionAssessment ? '-' : item.motorbike.conditionAssessment.electricalRating.label}"/></td>
                    </c:forEach>
                </tr>
            </table>
        </div>
    </c:if>

    <p><a href="${pageContext.request.contextPath}/products">← Danh sách sản phẩm</a></p>

    <script>
        // Tiện cho người dùng: báo ngay khi chọn quá số xe cho phép (máy chủ vẫn kiểm tra lại)
        (function () {
            var max = ${maxCompare};
            var form = document.getElementById('compareForm');
            if (!form) {
                return;
            }
            form.addEventListener('change', function (event) {
                var checked = form.querySelectorAll('input[name="id"]:checked');
                if (checked.length > max) {
                    event.target.checked = false;
                    alert('Bạn chỉ có thể so sánh tối đa ' + max + ' xe');
                }
            });
        })();
    </script>
</body>
</html>
