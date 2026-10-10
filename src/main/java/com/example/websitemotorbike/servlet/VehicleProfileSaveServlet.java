package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.VehicleProfileDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.model.VehicleProfile;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import com.example.websitemotorbike.util.VehicleCatalog;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Thêm hoặc sửa hồ sơ xe (POST /vehicles/save); có profileId là sửa. Được LoginRequiredFilter bảo vệ. */
@WebServlet("/vehicles/save")
public class VehicleProfileSaveServlet extends HttpServlet {

    private static final int MAX_NICKNAME = 50;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String profileId = request.getParameter("profileId");
        boolean editing = profileId != null && !profileId.trim().isEmpty();

        try {
            String nickname = request.getParameter("nickname");
            Validator.requireText(nickname, "Biệt danh");
            nickname = nickname.trim();
            if (nickname.length() > MAX_NICKNAME) {
                throw new IllegalArgumentException("Biệt danh quá dài (tối đa " + MAX_NICKNAME + " ký tự)");
            }

            // Ô chọn gửi "hãng|dòng xe"; chưa chọn thì báo đúng câu của SRS, chọn giá trị lạ thì báo không hợp lệ
            String vehicle = request.getParameter("vehicle");
            if (vehicle == null || vehicle.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn dòng xe");
            }
            int bar = vehicle.indexOf('|');
            String brand = bar < 0 ? "" : vehicle.substring(0, bar);
            String model = bar < 0 ? "" : vehicle.substring(bar + 1);
            if (!VehicleCatalog.contains(brand, model) && !isUnchangedModel(user, profileId, brand, model)) {
                throw new IllegalArgumentException("Dòng xe không hợp lệ, vui lòng chọn từ danh sách");
            }

            if (editing) {
                VehicleProfileDB.update(user.getAccountId(), profileId.trim(), nickname, brand, model);
                Flash.message(request, "Đã cập nhật xe");
            } else {
                VehicleProfileDB.create(user.getAccountId(), nickname, brand, model);
                Flash.message(request, "Đã thêm xe của bạn");
            }
            response.sendRedirect(request.getContextPath() + "/vehicles");
        } catch (IllegalArgumentException | IllegalStateException e) {
            showError(request, response, user, editing, e.getMessage());
        }
    }

    /** Khi sửa, cho giữ nguyên dòng xe cũ của chính hồ sơ này dù nó không còn trong danh sách chọn. */
    private boolean isUnchangedModel(User user, String profileId, String brand, String model) {
        if (profileId == null || profileId.trim().isEmpty()) {
            return false;
        }
        VehicleProfile current = VehicleProfileDB.selectOwned(user.getAccountId(), profileId.trim());
        return current != null && current.getBrand().equals(brand) && current.getModel().equals(model);
    }

    /** Lỗi dữ liệu: hiện lại trang với biểu mẫu giữ nguyên những gì khách đã nhập (quy ước README mục 6.2). */
    private void showError(HttpServletRequest request, HttpServletResponse response, User user,
                           boolean editing, String message) throws ServletException, IOException {
        request.setAttribute("error", message);
        if (editing) {
            VehicleProfile current = VehicleProfileDB.selectOwned(user.getAccountId(),
                    request.getParameter("profileId").trim());
            if (current == null) {
                Flash.error(request, "Không tìm thấy xe");
                response.sendRedirect(request.getContextPath() + "/vehicles");
                return;
            }
            request.setAttribute("editing", current);
        }
        VehicleProfileListServlet.showPage(request, response, user);
    }
}
