package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.VehicleProfileDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.model.VehicleProfile;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.VehicleCatalog;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;

/**
 * "Xe của tôi": danh sách hồ sơ xe của khách và biểu mẫu thêm hoặc sửa (SRS 3.15).
 * GET /vehicles, kèm ?edit=mã để sửa một hồ sơ. Được LoginRequiredFilter bảo vệ.
 */
@WebServlet("/vehicles")
public class VehicleProfileListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");

        String editId = request.getParameter("edit");
        if (editId != null && !editId.trim().isEmpty()) {
            VehicleProfile editing = VehicleProfileDB.selectOwned(user.getAccountId(), editId.trim());
            if (editing == null) {
                Flash.error(request, "Không tìm thấy xe");
                response.sendRedirect(request.getContextPath() + "/vehicles");
                return;
            }
            request.setAttribute("editing", editing);
        }

        Flash.consume(request);
        showPage(request, response, user);
    }

    /** Dùng chung cho doGet và VehicleProfileSaveServlet khi biểu mẫu báo lỗi. */
    static void showPage(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Map<String, TreeSet<String>> options = VehicleCatalog.options();

        // Hồ sơ đang sửa mà dòng xe đã không còn trong danh sách (ví dụ cửa hàng hết bán): vẫn cho chọn lại đúng giá trị cũ
        VehicleProfile editing = (VehicleProfile) request.getAttribute("editing");
        if (editing != null && !VehicleCatalog.contains(editing.getBrand(), editing.getModel())) {
            options.computeIfAbsent(editing.getBrand(), key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
                    .add(editing.getModel());
        }

        request.setAttribute("profiles", VehicleProfileDB.selectByAccountId(user.getAccountId()));
        request.setAttribute("options", options);
        request.setAttribute("maxProfiles", VehicleProfileDB.MAX_PER_ACCOUNT);
        request.getRequestDispatcher("/WEB-INF/views/vehicle-list.jsp").forward(request, response);
    }
}
