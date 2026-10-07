package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.OrderDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** "Đơn hàng của tôi": danh sách đơn của khách đang đăng nhập (SRS 3.12). */
@WebServlet("/orders")
public class OrderListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");

        Flash.consume(request);
        request.setAttribute("orders", OrderDB.selectByAccountId(user.getAccountId()));
        request.getRequestDispatcher("/WEB-INF/views/order-list.jsp").forward(request, response);
    }
}
