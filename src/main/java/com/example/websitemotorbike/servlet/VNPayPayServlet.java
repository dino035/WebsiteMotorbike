package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.OnlinePaymentDB;
import com.example.websitemotorbike.dao.OrderDB;
import com.example.websitemotorbike.model.OnlinePayment;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.VNPayConfig;
import com.example.websitemotorbike.util.VNPayUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Bắt đầu (hoặc làm lại) thanh toán VNPay cho một đơn của khách: POST id, rồi chuyển khách sang cổng VNPay. */
@WebServlet("/vnpay/pay")
public class VNPayPayServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String orderId = request.getParameter("id");
        Order order = (orderId == null || orderId.trim().isEmpty())
                ? null : OrderDB.selectOwned(user.getAccountId(), orderId.trim());
        if (order == null) {
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }
        String detailUrl = request.getContextPath() + "/orders/detail?id=" + order.getOrderId();

        if (!VNPayConfig.isConfigured()) {
            Flash.error(request, "Hệ thống chưa cấu hình VNPay, vui lòng chọn thanh toán khi nhận hàng (COD)");
            response.sendRedirect(detailUrl);
            return;
        }
        try {
            OnlinePayment attempt = OnlinePaymentDB.startAttempt(user.getAccountId(), order.getOrderId());
            response.sendRedirect(VNPayUtil.buildPaymentUrl(attempt, order.getShortCode(), VNPayUtil.clientIp(request)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Flash.error(request, e.getMessage());
            response.sendRedirect(detailUrl);
        }
    }
}
