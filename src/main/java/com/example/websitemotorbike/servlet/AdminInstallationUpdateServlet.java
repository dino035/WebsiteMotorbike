package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.InstallationDB;
import com.example.websitemotorbike.model.InstallationRequest;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.InstallationPolicy;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/** Admin đổi ngày, đánh dấu đã lắp đặt hoặc hủy một lịch lắp đặt (POST id, action, date). */
@WebServlet("/admin/installations/update")
public class AdminInstallationUpdateServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String id = request.getParameter("id");
            Validator.requireText(id, "Mã lịch lắp đặt");
            InstallationDB.Action action = parseAction(request.getParameter("action"));

            LocalDate newDate = null;
            if (action == InstallationDB.Action.RESCHEDULE) {
                newDate = InstallationPolicy.parse(request.getParameter("date"));
            }

            InstallationRequest installation = InstallationDB.update(id.trim(), action, newDate);
            Flash.message(request, "Đã cập nhật lịch lắp đặt của đơn #" + installation.getOrder().getShortCode()
                    + ": " + installation.getStatus().getLabel() + ", ngày " + installation.getScheduledDateText());
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/installations");
    }

    private InstallationDB.Action parseAction(String value) {
        try {
            return InstallationDB.Action.valueOf(value == null ? "" : value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Thao tác không hợp lệ");
        }
    }
}
