package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.OrderDB;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/orders/detail")
public class OrderDetailServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String id = request.getParameter("id");

        // Đơn không tồn tại hoặc không thuộc về khách này: về danh sách, không lộ đơn của người khác
        Order order = (id == null || id.trim().isEmpty())
                ? null : OrderDB.selectOwned(user.getAccountId(), id.trim());
        if (order == null) {
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }

        Flash.consume(request);
        request.setAttribute("order", order);
        request.setAttribute("items", OrderDB.selectItems(order.getOrderId()));
        request.setAttribute("payment", OrderDB.selectPayment(order.getOrderId()));
        request.getRequestDispatcher("/WEB-INF/views/order-detail.jsp").forward(request, response);
    }
}
