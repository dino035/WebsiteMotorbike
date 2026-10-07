package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.OrderDB;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.model.OrderStatus;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin đổi trạng thái đơn hàng (POST id, status). */
@WebServlet("/admin/orders/status")
public class AdminOrderStatusServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");

        try {
            Validator.requireText(id, "Mã đơn hàng");
            OrderStatus newStatus = parseStatus(request.getParameter("status"));

            Order order = OrderDB.updateStatus(id.trim(), newStatus);
            Flash.message(request, "Đã chuyển đơn #" + order.getShortCode() + " sang \""
                    + order.getStatus().getLabel() + "\"");
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/orders/detail?id="
                + (id == null ? "" : id.trim()));
    }

    private OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(value == null ? "" : value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }
    }
}
