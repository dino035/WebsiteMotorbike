<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Chỉnh sửa sản phẩm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />
<h1>Chỉnh sửa sản phẩm</h1>
<p style="color: red;"><c:out value="${error}"/></p>

<form action="edit?id=<c:out value='${product.productId}'/>" method="post" enctype="multipart/form-data" onsubmit="return checkPhotos(this)">
    <input type="hidden" name="id" value="${product.productId}">
    <input type="hidden" name="product" value="${product.typeCode}">
    <p>Loại sản phẩm: <strong>${product.productType}</strong></p>

    <label>Tên sản phẩm:</label>
    <input type="text" name="name" required value="<c:out value='${empty error ? product.name : param.name}'/>">

    <label>Giá sản phẩm:</label>
    <input type="number" name="price" min="0" required value="<c:out value='${empty error ? product.price : param.price}'/>">

    <label>Trạng thái:</label>
    <select name="status">
        <option value="AVAILABLE" ${(empty error ? product.status : param.status) == 'AVAILABLE' ? 'selected' : ''}>Có sẵn</option>
        <option value="SOLD" ${(empty error ? product.status : param.status) == 'SOLD' ? 'selected' : ''}>Hết hàng</option>
    </select>

    <c:if test="${product.typeCode == 'Motorbike'}">
        <div id="motorbikeFields">
            <label>Hãng xe:</label>
            <input type="text" name="brand" value="<c:out value='${empty error ? product.brand : param.brand}'/>" required>

            <label>Model:</label>
            <input type="text" name="model" value="<c:out value='${empty error ? product.model : param.model}'/>" required>

            <label>Năm đăng ký xe:</label>
            <input type="number" name="registrationYear" min="0" required value="<c:out value='${empty error ? product.registrationYear : param.registrationYear}'/>">

            <label>Số đời chủ:</label>
            <input type="number" name="ownerCount" min="0" required value="<c:out value='${empty error ? product.ownerCount : param.ownerCount}'/>">

            <label>Lịch sử tai nạn:</label>
            <input type="text" name="accidentHistory" value="<c:out value='${empty error ? product.accidentHistory : param.accidentHistory}'/>" required>

            <label>Số khung:</label>
            <input type="text" name="chassisNumber" value="<c:out value='${empty error ? product.chassisNumber : param.chassisNumber}'/>" required>

            <label>Số máy:</label>
            <input type="text" name="engineNumber" value="<c:out value='${empty error ? product.engineNumber : param.engineNumber}'/>" required>

            <label>Số km đã đi:</label>
            <input type="number" name="odometerAtIntake" min="0" required value="<c:out value='${empty error ? product.odometerAtIntake : param.odometerAtIntake}'/>">

            <h3>Đánh giá tình trạng xe</h3>
            <jsp:include page="/WEB-INF/views/common/rating-select.jsp">
                <jsp:param name="field" value="engineRating"/>
                <jsp:param name="label" value="Động cơ"/>
                <jsp:param name="selected" value="${empty error ? product.conditionAssessment.engineRating : param.engineRating}"/>
            </jsp:include>

            <jsp:include page="/WEB-INF/views/common/rating-select.jsp">
                <jsp:param name="field" value="brakeRating"/>
                <jsp:param name="label" value="Phanh"/>
                <jsp:param name="selected" value="${empty error ? product.conditionAssessment.brakeRating : param.brakeRating}"/>
            </jsp:include>

            <jsp:include page="/WEB-INF/views/common/rating-select.jsp">
                <jsp:param name="field" value="tireRating"/>
                <jsp:param name="label" value="Lốp"/>
                <jsp:param name="selected" value="${empty error ? product.conditionAssessment.tireRating : param.tireRating}"/>
            </jsp:include>

            <jsp:include page="/WEB-INF/views/common/rating-select.jsp">
                <jsp:param name="field" value="chainRating"/>
                <jsp:param name="label" value="Truyền động (xích/dây curoa)"/>
                <jsp:param name="selected" value="${empty error ? product.conditionAssessment.chainRating : param.chainRating}"/>
            </jsp:include>

            <jsp:include page="/WEB-INF/views/common/rating-select.jsp">
                <jsp:param name="field" value="electricalRating"/>
                <jsp:param name="label" value="Hệ thống điện"/>
                <jsp:param name="selected" value="${empty error ? product.conditionAssessment.electricalRating : param.electricalRating}"/>
            </jsp:include>

        </div>
    </c:if>

    <c:if test="${product.typeCode == 'SparePart'}">
        <div id="sparePartFields">
            <label>Loại phụ tùng:</label>
            <input type="text" name="partType" value="<c:out value='${empty error ? product.partType : param.partType}'/>" required>

            <label>Nguồn gốc:</label>
            <select name="partOrigin">
                <option value="GENUINE" ${(empty error ? product.partOrigin : param.partOrigin) == 'GENUINE' ? 'selected' : ''}>Chính hãng</option>
                <option value="AFTERMARKET" ${(empty error ? product.partOrigin : param.partOrigin) == 'AFTERMARKET' ? 'selected' : ''}>Phụ tùng bên thứ 3</option>
            </select>

        </div>
    </c:if>

    <c:if test="${product.typeCode == 'Motorbike' or product.typeCode == 'SparePart'}">
        <label>Ảnh hiện có (tick để xóa):</label>
        <div class="photo-gallery">
            <c:forEach var="photo" items="${photos}">
                <div>
                    <img src="<c:out value='${photo.imageUrl}'/>" alt="">
                    <br>
                    <input type="checkbox" name="removePhoto" value="<c:out value='${photo.photoId}'/>"> Xóa
                </div>
            </c:forEach>
        </div>

        <label>Thêm ảnh mới (tối đa 10 ảnh, mỗi ảnh ≤ 5MB):</label>
        <input type="file" name="photos" accept="image/jpeg,image/png,image/webp" multiple onchange="checkPhotoInput(this)"><br>
    </c:if>

            <input type = "submit" value = "Cập nhật">
</form>
<script>
    function checkPhotoInput(input) {
        var files = input.files;
        if (files.length > 10) {
            alert("Tối đa 10 ảnh");
            input.value = "";
            return false;
        }
        var allowed = ["image/jpeg", "image/png", "image/webp"];
        for (var i = 0; i < files.length; i++) {
            if (allowed.indexOf(files[i].type) === -1) {
                alert("Ảnh \"" + files[i].name + "\" không đúng định dạng, chỉ nhận JPG, PNG hoặc WebP");
                input.value = "";
                return false;
            }
            if (files[i].size > 5 * 1024 * 1024) {
                alert("Ảnh \"" + files[i].name + "\" lớn hơn 5MB, vui lòng chọn ảnh khác");
                input.value = "";
                return false;
            }
        }
        return true;
    }

    function checkPhotos(form) {
        var input = form.querySelector('input[type="file"]');
        return !input || checkPhotoInput(input);
    }
</script>
</body>
</html>
