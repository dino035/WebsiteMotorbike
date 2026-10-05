<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Thêm sản phẩm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<h1>Chỉnh sửa sản phẩm</h1>

<form action = "edit" method = "post">

    <input type="hidden" name="id" value="${product.productId}">

    <select name="product" id="productSelect" onchange="toggleFields()">
        <option value="Motorbike" ${product.typeCode == 'Motorbike' ? 'selected' : ''}>Xe cũ</option>
        <option value="SparePart" ${product.typeCode == 'SparePart' ? 'selected' : ''}>Phụ tùng</option>
    </select>

    <label>Tên sản phẩm:</label>
    <input type = "text" name = "name" required value="${product.name}"><br>

    <label>Giá sản phẩm:</label>
    <input type = "text" name = "price" required value="${product.price}"><br>

    <c:if test="${product.typeCode == 'Motorbike'}">
        <div id="motorbikeFields">
            <label>Hãng xe:</label>
            <input type = "text" name = "brand" required value="${product.brand}"><br>

            <label>Model:</label>
            <input type = "text" name = "model" required value="${product.model}"><br>

            <label>Năm đăng ký xe:</label>
            <input type = "number" name = "registrationYear" required value="${product.registrationYear}"><br>

            <label>Số đời chủ:</label>
            <input type = "number" name = "ownerCount" required value="${product.ownerCount}"><br>

            <label>Lịch sử tai nạn:</label>
            <input type = "text" name = "accidentHistory" required value="${product.accidentHistory}"><br>

            <label>Số khung:</label>
            <input type = "text" name = "chassisNumber" required value="${product.chassisNumber}"><br>

            <label>Số máy:</label>
            <input type = "text" name = "engineNumber" required value="${product.engineNumber}"><br>

            <label>Số odor:</label>
            <input type = "number" name = "odometerAtIntake" required value="${product.odometerAtIntake}"><br>

        </div>
    </c:if>

    <c:if test="${product.typeCode == 'SparePart'}">
        <div id="sparePartFields" style="display: none;">
            <label>Loại phụ tùng:</label>
            <input type="text" name = "partType" required value="${product.partType}"><br>

            <label>Nguồn gốc:</label>
            <select name="partOrigin">
                <option value="GENUINE" ${product.partOrigin == 'GENUINE' ? 'selected' : ''}>Phụ tùng chính hãng</option>
                <option value="AFTERMARKET" ${product.partOrigin == 'AFTERMARKET' ? 'selected' : ''}>Phụ tùng bên thứ 3</option>
            </select>

        </div>
    </c:if>

            <input type = "submit" value = "Cập nhật">
</form>
<script>
    function toggleFields() {
        var productType = document.getElementById("productSelect").value;

        var motorbikeDiv = document.getElementById("motorbikeFields");
        var sparePartDiv = document.getElementById("sparePartFields");

        if (motorbikeDiv) {
            var motorbikeInputs = motorbikeDiv.querySelectorAll("input");
            if (productType === "Motorbike") {
                motorbikeDiv.style.display = "block";
                motorbikeInputs.forEach(function(input) { input.required = true; });
            } else {
                motorbikeDiv.style.display = "none";
                motorbikeInputs.forEach(function(input) { input.required = false; });
            }
        }

        if (sparePartDiv) {
            var sparePartInputs = sparePartDiv.querySelectorAll("input, select");
            if (productType === "SparePart") {
                sparePartDiv.style.display = "block";
                sparePartInputs.forEach(function(input) { input.required = true; });
            } else {
                sparePartDiv.style.display = "none";
                sparePartInputs.forEach(function(input) { input.required = false; });
            }
        }
    }
    toggleFields();
</script>

</body>
</html>
