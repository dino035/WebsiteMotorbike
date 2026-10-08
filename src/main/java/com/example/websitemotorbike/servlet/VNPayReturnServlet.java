package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.OnlinePaymentDB;
import com.example.websitemotorbike.util.VNPayConfig;
import com.example.websitemotorbike.util.VNPayUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * VNPay đưa trình duyệt của khách quay về đây (GET) sau khi trả tiền, kèm các tham số vnp_* đã ký.
 * Trang này không đòi đăng nhập vì kết quả được tin nhờ chữ ký, không nhờ phiên đăng nhập.
 */
@WebServlet("/vnpay/return")
public class VNPayReturnServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!VNPayConfig.isConfigured()) {
            request.setAttribute("problem", "Hệ thống chưa cấu hình VNPay");
            forward(request, response);
            return;
        }

        Map<String, String> params = VNPayUtil.extractParams(request);
        if (!VNPayUtil.verifySignature(params) || !VNPayConfig.tmnCode().equals(params.get("vnp_TmnCode"))) {
            request.setAttribute("problem", "Chữ ký không hợp lệ, không thể xác nhận kết quả thanh toán");
            forward(request, response);
            return;
        }

        try {
            OnlinePaymentDB.Result result = OnlinePaymentDB.complete(params);
            request.setAttribute("outcome", result.getOutcome().name());
            request.setAttribute("order", result.getOrder());
            request.setAttribute("attempt", result.getAttempt());
            request.setAttribute("reason", VNPayUtil.describeResponse(params.get("vnp_ResponseCode")));
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.setAttribute("problem", e.getMessage());
        }
        forward(request, response);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/vnpay-result.jsp").forward(request, response);
    }
}
