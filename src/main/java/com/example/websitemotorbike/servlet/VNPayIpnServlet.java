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
 * IPN (Instant Payment Notification): VNPay gọi thẳng đến máy chủ để báo kết quả, kể cả khi khách đóng trình duyệt
 * trước khi quay về. Cần địa chỉ công khai nên khi chạy ở localhost VNPay không gọi tới được; kết quả vẫn được ghi
 * nhận khi trình duyệt quay về /vnpay/return. Cả hai đường dùng chung OnlinePaymentDB.complete nên không ghi trùng.
 * Trả về JSON theo quy định: RspCode 00 là đã nhận, các mã khác báo lỗi.
 */
@WebServlet("/vnpay/ipn")
public class VNPayIpnServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String code;
        String message;
        Map<String, String> params = VNPayUtil.extractParams(request);

        if (!VNPayConfig.isConfigured()) {
            code = "99";
            message = "Not configured";
        } else if (!VNPayUtil.verifySignature(params) || !VNPayConfig.tmnCode().equals(params.get("vnp_TmnCode"))) {
            code = "97";
            message = "Invalid signature";
        } else {
            try {
                switch (OnlinePaymentDB.complete(params).getOutcome()) {
                    case NOT_FOUND:
                        code = "01";
                        message = "Order not found";
                        break;
                    case AMOUNT_MISMATCH:
                        code = "04";
                        message = "Invalid amount";
                        break;
                    case ALREADY_PROCESSED:
                        code = "02";
                        message = "Order already confirmed";
                        break;
                    default:
                        code = "00";
                        message = "Confirm Success";
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                code = "99";
                message = "Unknown error";
            }
        }

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"RspCode\":\"" + code + "\",\"Message\":\"" + message + "\"}");
    }
}
