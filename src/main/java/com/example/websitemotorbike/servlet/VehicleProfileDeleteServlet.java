package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.VehicleProfileDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Xóa hồ sơ xe của khách (POST /vehicles/delete, tham số id). Được LoginRequiredFilter bảo vệ. */
@WebServlet("/vehicles/delete")
public class VehicleProfileDeleteServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String id = request.getParameter("id");

        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy xe");
            }
            VehicleProfileDB.delete(user.getAccountId(), id.trim());
            Flash.message(request, "Đã xóa xe");
        } catch (IllegalArgumentException | IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/vehicles");
    }
}
