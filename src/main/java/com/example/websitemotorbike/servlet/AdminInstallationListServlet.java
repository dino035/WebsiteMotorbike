package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.InstallationDB;
import com.example.websitemotorbike.model.InstallationStatus;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.InstallationPolicy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Admin xem lịch lắp đặt tại cửa hàng, lọc theo trạng thái (SRS 3.21).
 * Được AdminFilter bảo vệ (/admin/*).
 */
@WebServlet("/admin/installations")
public class AdminInstallationListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        InstallationStatus status = parseStatus(request.getParameter("status"));

        Flash.consume(request);
        request.setAttribute("installations", InstallationDB.selectAll(status));
        request.setAttribute("statuses", InstallationStatus.values());
        request.setAttribute("selectedStatus", status == null ? "" : status.name());
        request.setAttribute("maxPerDay", InstallationPolicy.MAX_PER_DAY);
        request.setAttribute("minDate", LocalDate.now().plusDays(1).toString());
        request.setAttribute("maxDate", LocalDate.now().plusDays(InstallationPolicy.MAX_DAYS_AHEAD).toString());
        request.getRequestDispatcher("/WEB-INF/views/admin-installation-list.jsp").forward(request, response);
    }

    /** Giá trị lọc không hợp lệ thì bỏ qua (xem tất cả) thay vì báo lỗi. */
    private InstallationStatus parseStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return InstallationStatus.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
