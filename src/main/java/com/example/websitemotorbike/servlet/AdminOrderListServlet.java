package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.OrderDB;
import com.example.websitemotorbike.model.OrderStatus;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin xem danh sách mọi đơn hàng, có thể lọc theo trạng thái (SRS 3.21). Được AdminFilter bảo vệ (/admin/*). */
@WebServlet("/admin/orders")
public class AdminOrderListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        OrderStatus status = parseStatus(request.getParameter("status"));

        Flash.consume(request);
        request.setAttribute("orders", OrderDB.selectAll(status));
        request.setAttribute("statuses", OrderStatus.values());
        request.setAttribute("selectedStatus", status == null ? "" : status.name());
        request.getRequestDispatcher("/WEB-INF/views/admin-order-list.jsp").forward(request, response);
    }

    /** Giá trị lọc không hợp lệ thì bỏ qua (xem tất cả) thay vì báo lỗi. */
    private OrderStatus parseStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
