package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.InstallationDB;
import com.example.websitemotorbike.dao.OnlinePaymentDB;
import com.example.websitemotorbike.dao.OrderDB;
import com.example.websitemotorbike.dao.UserDB;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/admin/orders/detail")
public class AdminOrderDetailServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");
        Order order = (id == null || id.trim().isEmpty()) ? null : OrderDB.selectById(id.trim());
        if (order == null) {
            response.sendRedirect(request.getContextPath() + "/admin/orders");
            return;
        }

        Flash.consume(request);
        request.setAttribute("order", order);
        request.setAttribute("items", OrderDB.selectItems(order.getOrderId()));
        request.setAttribute("payment", OrderDB.selectPayment(order.getOrderId()));
        request.setAttribute("installation", InstallationDB.selectByOrderId(order.getOrderId()));
        request.setAttribute("attempts", OnlinePaymentDB.selectByOrderId(order.getOrderId()));
        request.setAttribute("customer", UserDB.selectByAccountId(order.getAccountId()));
        request.getRequestDispatcher("/WEB-INF/views/admin-order-detail.jsp").forward(request, response);
    }
}
