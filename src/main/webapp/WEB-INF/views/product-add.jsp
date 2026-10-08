<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Thêm sản phẩm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/common/header.jsp" />

    <h1>Thêm sản phẩm</h1>
    <p style="color: red;"><c:out value="${error}"/></p>

    <form action="add" method="post" enctype="multipart/form-data" onsubmit="return checkPhotos(this)">
        <select name="product" id="productSelect" onchange="toggleFields()">
            <option value="Motorbike" ${param.product != 'SparePart' ? 'selected' : ''}>Xe cũ</option>
            <option value="SparePart" ${param.product == 'SparePart' ? 'selected' : ''}>Phụ tùng</option>
        </select>

        <label>Tên sản phẩm:</label>
        <input type="text" name="name" required value="<c:out value='${param.name}'/>">

        <label>Giá sản phẩm:</label>
        <input type="number" name="price" min="0" required value="<c:out value='${param.price}'/>">

        <label>Trạng thái:</label>
        <select name="status">
            <option value="AVAILABLE" ${param.status != 'SOLD' ? 'selected' : ''}>Có sẵn</option>
            <option value="SOLD" ${param.status == 'SOLD' ? 'selected' : ''}>Hết hàng</option>
        </select>

        <div id="motorbikeFields">
            <label>Hãng xe:</label>
            <input type = "text" name = "brand" value="<c:out value='${param.brand}'/>" required><br>

            <label>Model:</label>
            <input type = "text" name = "model" value="<c:out value='${param.model}'/>" required><br>

            <label>Năm đăng ký xe:</label>
            <input type = "number" name = "registrationYear" min="0" value="<c:out value='${param.registrationYear}'/>" required><br>


            <label>Số đời chủ:</label>
            <input type = "number" name = "ownerCount" min="0" value="<c:out value='${param.ownerCount}'/>" required><br>

            <label>Lịch sử tai nạn:</label>
            <input type = "text" name = "accidentHistory" value="<c:out value='${param.accidentHistory}'/>" required><br>


            <label>Số khung:</label>
            <input type = "text" name = "chassisNumber" value="<c:out value='${param.chassisNumber}'/>" required><br>


            <label>Số máy:</label>
            <input type = "text" name = "engineNumber" value="<c:out value='${param.engineNumber}'/>" required><br>


            <label>Số km đã đi:</label>
            <input type = "number" name = "odometerAtIntake" min="0" value="<c:out value='${param.odometerAtIntake}'/>" required><br>

            <label>Ảnh xe (tối đa 10 ảnh, mỗi ảnh ≤ 5MB):</label>
            <input type="file" name="photos" accept="image/jpeg,image/png,image/webp" multiple required onchange="checkPhotoInput(this)"><br>
        </div>

        <div id="sparePartFields" style="display: none;">
            <label>Loại phụ tùng:</label>
            <input type = "text" name = "partType" value="<c:out value='${param.partType}'/>" required><br>


            <label>Nguồn gốc:</label>
            <select name="partOrigin">
                <option value="GENUINE" ${param.partOrigin != 'AFTERMARKET' ? 'selected' : ''}>Chính hãng</option>
                <option value="AFTERMARKET" ${param.partOrigin == 'AFTERMARKET' ? 'selected' : ''}>Phụ tùng bên thứ 3</option>
            </select>
        </div>

        <input type = "submit" value = "Thêm">
    </form>

<script>
    function toggleFields() {
        var productType = document.getElementById("productSelect").value;

        var motorbikeDiv = document.getElementById("motorbikeFields");
        var sparePartDiv = document.getElementById("sparePartFields");

        var motorbikeInputs = motorbikeDiv.querySelectorAll("input");
        var sparePartInputs = sparePartDiv.querySelectorAll("input, select");

        if (productType === "Motorbike") {
            motorbikeDiv.style.display = "block";
            sparePartDiv.style.display = "none";

            motorbikeInputs.forEach(function(input) { input.required = true; });
            sparePartInputs.forEach(function(input) { input.required = false; });
        } else {
            motorbikeDiv.style.display = "none";
            sparePartDiv.style.display = "block";

            motorbikeInputs.forEach(function(input) { input.required = false; });
            sparePartInputs.forEach(function(input) { input.required = true; });
        }
    }
    function checkPhotoInput(input) {
        var files = input.files;
        if (files.length > 10) {
            alert("Tối đa 10 ảnh cho mỗi xe");
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

    toggleFields();
</script>
</body>
</html>
