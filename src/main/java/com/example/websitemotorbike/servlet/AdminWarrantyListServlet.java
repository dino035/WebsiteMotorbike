package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.WarrantyDB;
import com.example.websitemotorbike.model.WarrantyStatus;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin xem danh sách yêu cầu bảo hành, lọc theo trạng thái (SRS 3.22). Được AdminFilter bảo vệ (/admin/*). */
@WebServlet("/admin/warranty")
public class AdminWarrantyListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        WarrantyStatus status = parseStatus(request.getParameter("status"));

        Flash.consume(request);
        request.setAttribute("requests", WarrantyDB.selectAll(status));
        request.setAttribute("statuses", WarrantyStatus.values());
        request.setAttribute("selectedStatus", status == null ? "" : status.name());
        request.getRequestDispatcher("/WEB-INF/views/admin-warranty-list.jsp").forward(request, response);
    }

    /** Giá trị lọc không hợp lệ thì bỏ qua (xem tất cả) thay vì báo lỗi. */
    private WarrantyStatus parseStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return WarrantyStatus.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
