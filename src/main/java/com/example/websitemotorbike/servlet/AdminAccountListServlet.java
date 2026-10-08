package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.AccountLockDB;
import com.example.websitemotorbike.dao.UserDB;
import com.example.websitemotorbike.model.AccountLock;
import com.example.websitemotorbike.model.AccountRow;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin xem danh sách tài khoản khách hàng, tìm theo email, lọc theo trạng thái (SRS 3.23).
 * Được AdminFilter bảo vệ (/admin/*).
 */
@WebServlet("/admin/accounts")
public class AdminAccountListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String keyword = request.getParameter("q");
        String status = normalizeStatus(request.getParameter("status"));

        Map<String, AccountLock> locks = AccountLockDB.selectAllLocks();
        Map<String, Long> unfinished = AccountLockDB.countUnfinishedOrders();

        List<AccountRow> rows = new ArrayList<>();
        for (User user : UserDB.selectCustomers(keyword)) {
            AccountLock lock = locks.get(user.getAccountId());
            if (("ACTIVE".equals(status) && lock != null) || ("LOCKED".equals(status) && lock == null)) {
                continue;
            }
            rows.add(new AccountRow(user, lock, unfinished.getOrDefault(user.getAccountId(), 0L)));
        }

        Flash.consume(request);
        request.setAttribute("rows", rows);
        request.setAttribute("selectedStatus", status);
        request.getRequestDispatcher("/WEB-INF/views/admin-account-list.jsp").forward(request, response);
    }

    /** Giá trị lọc lạ thì coi như xem tất cả. */
    private String normalizeStatus(String value) {
        if ("ACTIVE".equals(value) || "LOCKED".equals(value)) {
            return value;
        }
        return "ALL";
    }
}
