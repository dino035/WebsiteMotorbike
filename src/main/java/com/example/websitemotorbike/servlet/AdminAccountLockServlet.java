package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.AccountLockDB;
import com.example.websitemotorbike.dao.UserDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin khóa một tài khoản khách hàng (POST id, reason, force). */
@WebServlet("/admin/accounts/lock")
public class AdminAccountLockServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User admin = (User) request.getSession().getAttribute("currentUser");

        try {
            String id = request.getParameter("id");
            Validator.requireText(id, "Mã tài khoản");

            // force = ô xác nhận "vẫn khóa" khi tài khoản còn đơn hàng chưa hoàn tất
            boolean force = request.getParameter("force") != null;
            AccountLockDB.lock(admin.getAccountId(), id.trim(), request.getParameter("reason"), force);

            User target = UserDB.selectByAccountId(id.trim());
            Flash.message(request, "Đã khóa tài khoản " + (target == null ? "" : target.getEmail()));
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/accounts");
    }
}
