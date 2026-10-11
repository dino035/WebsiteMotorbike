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

<div class="detail-grid">

    <%-- Cột trái: thư viện ảnh (có mũi tên khi từ 2 ảnh trở lên) --%>
    <div class="detail-gallery">
        <c:choose>
            <c:when test="${not empty photos}">
                <div class="carousel">
                    <div class="slide-frame">
                        <c:forEach var="photo" items="${photos}" begin="0" end="0">
                            <img id="mainPhoto" class="gallery-main"
                                 src="<c:out value='${photo.imageUrl}'/>"
                                 alt="<c:out value='${product.name}'/>">
                        </c:forEach>
                        <c:if test="${photos.size() > 1}">
                            <button type="button" class="nav prev" id="btnPrev" aria-label="Ảnh trước">&#10094;</button>
                            <button type="button" class="nav next" id="btnNext" aria-label="Ảnh sau">&#10095;</button>
                        </c:if>
                    </div>
                    <c:if test="${photos.size() > 1}">
                        <div class="dots">
                            <c:forEach var="photo" items="${photos}" varStatus="st">
                                <button type="button" class="dot ${st.first ? 'active' : ''}"
                                        data-src="<c:out value='${photo.imageUrl}'/>"
                                        aria-label="Ảnh ${st.count}"></button>
                            </c:forEach>
                        </div>
                    </c:if>
                </div>
            </c:when>
            <c:otherwise>
                <div class="gallery-main gallery-empty">Chưa có ảnh</div>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- Cột phải: thông tin và mua hàng --%>
    <div class="detail-info">
        <div class="price-big"><fmt:formatNumber value="${product.price}" type="number" groupingUsed="true"/> ₫</div>
        <p>
            <c:choose>
                <c:when test="${product.status == 'AVAILABLE'}">
                    <span class="badge badge-ok"><c:out value="${product.status.label}"/></span>
                </c:when>
                <c:otherwise>
                    <span class="badge badge-sold"><c:out value="${product.status.label}"/></span>
                </c:otherwise>
            </c:choose>
        </p>

        <table>
            <tr><th>Loại</th><td><c:out value="${product.productType}"/></td></tr>

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
                    <p><a class="btn" href="${pageContext.request.contextPath}/login">Đăng nhập để mua hàng</a></p>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</div>

<%-- Đánh giá tình trạng xe --%>
<c:if test="${product.typeCode == 'Motorbike'}">
    <h2>Đánh giá tình trạng xe</h2>
    <c:choose>
        <c:when test="${not empty product.conditionAssessment}">
            <c:set var="assessment" value="${product.conditionAssessment}"/>
            <div class="assess-wrap">
                <div class="assess-score">
                    <div class="score-num">
                        <fmt:formatNumber value="${assessment.adjustmentCoefficient}" minFractionDigits="1" maxFractionDigits="1"/><span>/10</span>
                    </div>
                    <div class="score-label">Điểm tình trạng</div>
                    <div class="score-verdict"><c:out value="${assessment.verdict}"/></div>
                </div>
                <div class="assess-grid">
                    <div class="assess-item"><div class="k">Động cơ</div><div class="v"><c:out value="${assessment.engineRating.label}"/></div></div>
                    <div class="assess-item"><div class="k">Phanh</div><div class="v"><c:out value="${assessment.brakeRating.label}"/></div></div>
                    <div class="assess-item"><div class="k">Lốp</div><div class="v"><c:out value="${assessment.tireRating.label}"/></div></div>
                    <div class="assess-item"><div class="k">Truyền động (xích/dây curoa)</div><div class="v"><c:out value="${assessment.chainRating.label}"/></div></div>
                    <div class="assess-item"><div class="k">Hệ thống điện</div><div class="v"><c:out value="${assessment.electricalRating.label}"/></div></div>
                </div>
            </div>
            <c:if test="${not empty assessment.repairAdvice}">
                <div class="advice">
                    <strong>Hạng mục cần lưu ý</strong>
                    <ul>
                        <c:forEach var="tip" items="${assessment.repairAdvice}">
                            <li><c:out value="${tip}"/></li>
                        </c:forEach>
                    </ul>
                </div>
            </c:if>
        </c:when>
        <c:otherwise>
            <p>Chưa có đánh giá tình trạng.</p>
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
            <div class="review-card">
                <div class="review-head">
                    <strong>Khách hàng</strong>
                    <span class="stars">★ <c:out value="${r.rating}"/>/5</span>
                    <small>(<c:out value="${r.createdAtDisplay}"/>)</small>
                </div>
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
            <label>Nhận xét:
                <textarea name="comment" rows="4" maxlength="1000" required
                          placeholder="Nhận xét của bạn về sản phẩm"></textarea>
            </label>
            <button type="submit">Gửi đánh giá</button>
        </form>
    </c:when>
    <c:otherwise>
        <p>Bạn chỉ có thể đánh giá sản phẩm đã mua.</p>
    </c:otherwise>
</c:choose>

<p><a href="${pageContext.request.contextPath}/products">← Quay lại danh sách</a></p>

<script>
    (function () {
        var img = document.getElementById('mainPhoto');
        var dots = document.querySelectorAll('.dots .dot');
        if (!img || dots.length < 2) return;
        var cur = 0;

        function show(i) {
            cur = (i + dots.length) % dots.length;
            img.src = dots[cur].getAttribute('data-src');
            for (var k = 0; k < dots.length; k++) {
                dots[k].classList.toggle('active', k === cur);
            }
        }

        document.getElementById('btnPrev').onclick = function () { show(cur - 1); };
        document.getElementById('btnNext').onclick = function () { show(cur + 1); };
        dots.forEach(function (d, idx) {
            d.onclick = function () { show(idx); };
        });

        document.addEventListener('keydown', function (e) {
            var t = e.target.tagName;
            if (t === 'INPUT' || t === 'TEXTAREA' || t === 'SELECT') return;
            if (e.key === 'ArrowLeft') show(cur - 1);
            if (e.key === 'ArrowRight') show(cur + 1);
        });
    })();
</script>
</body>
</html>