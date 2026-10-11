package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.dao.VehiclePhotoDB;
import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.ProductStatus;
import com.example.websitemotorbike.model.VehiclePhoto;
import com.example.websitemotorbike.util.CompareItem;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * So sánh song song tối đa 4 xe máy (SRS 3.8). Trang công khai, không cần đăng nhập.
 * GET /compare hiện danh sách xe còn bán để chọn; GET /compare?id=...&id=... (2 đến 4 mã) hiện thêm bảng so sánh.
 * Dữ liệu từng xe lấy qua ProductDB của Nhóm 2 (README mục 3), không tự viết lại truy vấn Product.
 */
@WebServlet("/compare")
public class CompareServlet extends HttpServlet {

    public static final int MAX_COMPARE = 4;
    public static final int MIN_COMPARE = 2;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Set<String> ids = readIds(request.getParameterValues("id"));
        String error = null;
        List<CompareItem> items = new ArrayList<>();

        if (ids.size() > MAX_COMPARE) {
            // Kiểm tra số lượng trước khi chạm vào CSDL, để danh sách mã quá dài không làm tốn truy vấn
            error = "Bạn chỉ có thể so sánh tối đa " + MAX_COMPARE + " xe, vui lòng bỏ bớt lựa chọn";
        } else if (ids.size() == 1) {
            error = "Vui lòng chọn ít nhất " + MIN_COMPARE + " xe để so sánh";
        } else if (ids.size() >= MIN_COMPARE) {
            for (String id : ids) {
                Product product = ProductDB.selectById(id);
                if (!(product instanceof Motorbike)) {
                    error = "Có xe không tồn tại hoặc không phải xe máy, vui lòng chọn lại";
                    items.clear();
                    break;
                }
                List<VehiclePhoto> photos = VehiclePhotoDB.selectByMotorbikeId(id);
                items.add(new CompareItem((Motorbike) product, photos.isEmpty() ? null : photos.get(0).getImageUrl()));
            }
            if (error == null) {
                CompareItem.markBest(items);
            }
        }

        request.setAttribute("error", error);
        request.setAttribute("items", items);
        request.setAttribute("selectedIds", ids);
        request.setAttribute("maxCompare", MAX_COMPARE);
        request.setAttribute("minCompare", MIN_COMPARE);
        request.setAttribute("bikes", ProductDB.search(null, "Motorbike", ProductStatus.AVAILABLE, null, null));
        request.getRequestDispatcher("/WEB-INF/views/compare.jsp").forward(request, response);
    }

    /** Các mã xe người dùng chọn: bỏ mã trống và mã trùng, giữ thứ tự chọn. */
    private Set<String> readIds(String[] values) {
        Set<String> ids = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    ids.add(value.trim());
                }
            }
        }
        return ids;
    }
}
