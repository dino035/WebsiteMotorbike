package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.OrderDB;
import com.example.websitemotorbike.model.DeliveryMethod;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.model.PaymentMethod;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.CartSummary;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.regex.Pattern;

@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private static final String CHECKOUT_VIEW = "/WEB-INF/views/checkout.jsp";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+ ().-]{8,15}$");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        CartSummary summary = CartSummary.load(user.getAccountId());

        if (summary.getLines().isEmpty()) {
            Flash.error(request, "Giỏ hàng của bạn đang trống, hãy thêm sản phẩm trước khi thanh toán");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }
        showForm(request, response, summary, user);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");

        try {
            String receiverName = request.getParameter("receiverName");
            String receiverPhone = request.getParameter("receiverPhone");
            String shippingAddress = request.getParameter("shippingAddress");

            Validator.requireText(receiverName, "Họ tên người nhận");
            Validator.requireText(receiverPhone, "Số điện thoại");
            if (receiverName.trim().length() > 100) {
                throw new IllegalArgumentException("Họ tên người nhận quá dài (tối đa 100 ký tự)");
            }
            if (!PHONE_PATTERN.matcher(receiverPhone.trim()).matches()) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ");
            }

            DeliveryMethod deliveryMethod = parseDeliveryMethod(request.getParameter("deliveryMethod"));
            if (deliveryMethod == DeliveryMethod.DELIVERY) {
                Validator.requireText(shippingAddress, "Địa chỉ giao hàng");
                if (shippingAddress.trim().length() > 500) {
                    throw new IllegalArgumentException("Địa chỉ giao hàng quá dài (tối đa 500 ký tự)");
                }
            }

            PaymentMethod paymentMethod = parsePaymentMethod(request.getParameter("paymentMethod"));
            if (paymentMethod != PaymentMethod.COD) {
                throw new IllegalArgumentException(
                        "Thanh toán VNPay chưa được hỗ trợ, vui lòng chọn thanh toán khi nhận hàng (COD)");
            }

            if (request.getParameter("acceptPolicy") == null) {
                throw new IllegalArgumentException("Vui lòng xác nhận chính sách đổi trả để tiếp tục");
            }

            String address = deliveryMethod == DeliveryMethod.DELIVERY ? shippingAddress.trim() : null;
            Order order = OrderDB.placeOrder(user.getAccountId(), deliveryMethod,
                    receiverName.trim(), receiverPhone.trim(), address, paymentMethod, true);

            Flash.message(request, "Đặt hàng thành công! Mã đơn hàng: " + order.getShortCode());
            response.sendRedirect(request.getContextPath() + "/orders/detail?id=" + order.getOrderId());
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            showForm(request, response, CartSummary.load(user.getAccountId()), user);
        } catch (IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
            showForm(request, response, CartSummary.load(user.getAccountId()), user);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          CartSummary summary, User user) throws ServletException, IOException {
        request.setAttribute("lines", summary.getLines());
        request.setAttribute("total", summary.getTotal());
        request.setAttribute("notices", summary.getNotices());
        request.setAttribute("defaultPhone", user.getPhone());
        request.setAttribute("deliveryMethods", DeliveryMethod.values());
        request.setAttribute("paymentMethods", PaymentMethod.values());
        request.getRequestDispatcher(CHECKOUT_VIEW).forward(request, response);
    }

    private DeliveryMethod parseDeliveryMethod(String value) {
        try {
            return DeliveryMethod.valueOf(value == null ? "" : value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Vui lòng chọn hình thức nhận hàng");
        }
    }

    private PaymentMethod parsePaymentMethod(String value) {
        try {
            return PaymentMethod.valueOf(value == null ? "" : value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán");
        }
    }
}
