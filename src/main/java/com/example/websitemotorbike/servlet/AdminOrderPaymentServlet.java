package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.OrderDB;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin xác nhận đã thu tiền COD của một đơn hàng (POST id). */
@WebServlet("/admin/orders/payment")
public class AdminOrderPaymentServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");

        try {
            Validator.requireText(id, "Mã đơn hàng");
            Order order = OrderDB.confirmPayment(id.trim());
            Flash.message(request, "Đã xác nhận thu tiền cho đơn #" + order.getShortCode());
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/orders/detail?id="
                + (id == null ? "" : id.trim()));
    }
}
