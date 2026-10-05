<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Thêm sản phẩm</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <h1>Thêm sản phẩm</h1>
    <p style="color: red;">${error}</p>

    <form action = "add" method = "post">
        <select name="product" id="productSelect" onchange="toggleFields()">
            <option value="Motorbike" selected>Xe cũ</option>
            <option value="SparePart">Phụ tùng</option>
        </select>

        <label>Tên sản phẩm:</label>
        <input type = "text" name = "name" required><br>

        <label>Giá sản phẩm:</label>
        <input type = "number" name = "price" min="0" required><br>

        <div id="motorbikeFields">
            <label>Hãng xe:</label>
            <input type = "text" name = "brand" required><br>

            <label>Model:</label>
            <input type = "text" name = "model" required><br>

            <label>Năm đăng ký xe:</label>
            <input type = "number" name = "registrationYear" min="0" required><br>

            <label>Số đời chủ:</label>
            <input type = "number" name = "ownerCount" min="0" required><br>

            <label>Lịch sử tai nạn:</label>
            <input type = "text" name = "accidentHistory" required><br>

            <label>Số khung:</label>
            <input type = "text" name = "chassisNumber" required><br>

            <label>Số máy:</label>
            <input type = "text" name = "engineNumber" required><br>

            <label>Số odor:</label>
            <input type = "number" name = "odometerAtIntake" min="0" required><br>

        </div>

        <div id="sparePartFields" style="display: none;">
            <label>Loại phụ tùng:</label>
            <input type="text" name = "partType" required><br>

            <label>Nguồn gốc:</label>
            <select name="partOrigin">
                <option value="GENUINE" selected>Phụ tùng chính hãng</option>
                <option value="AFTERMARKET">Phụ tùng bên thứ 3 </option>
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
        toggleFields();
    </script>
</body>
</html>
