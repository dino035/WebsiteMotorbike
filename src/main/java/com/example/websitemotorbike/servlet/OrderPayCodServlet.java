package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.OnlinePaymentDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Khách đổi một đơn chưa trả tiền VNPay sang thanh toán khi nhận hàng (POST id). */
@WebServlet("/orders/pay-cod")
public class OrderPayCodServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String orderId = request.getParameter("id");
        if (orderId == null || orderId.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }
        try {
            OnlinePaymentDB.useCod(user.getAccountId(), orderId.trim());
            Flash.message(request, "Đã chuyển sang thanh toán khi nhận hàng (COD)");
        } catch (IllegalArgumentException | IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/orders/detail?id=" + orderId.trim());
    }
}
