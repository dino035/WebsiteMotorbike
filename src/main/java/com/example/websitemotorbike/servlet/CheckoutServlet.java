package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.AddressDB;
import com.example.websitemotorbike.dao.OnlinePaymentDB;
import com.example.websitemotorbike.dao.OrderDB;
import com.example.websitemotorbike.model.Address;
import com.example.websitemotorbike.model.CartLine;
import com.example.websitemotorbike.model.DeliveryMethod;
import com.example.websitemotorbike.model.OnlinePayment;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.model.PaymentMethod;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.CartSummary;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.InputRules;
import com.example.websitemotorbike.util.InstallationPolicy;
import com.example.websitemotorbike.util.VNPayConfig;
import com.example.websitemotorbike.util.VNPayUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private static final String CHECKOUT_VIEW = "/WEB-INF/views/checkout.jsp";

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

            InputRules.validateReceiver(receiverName, receiverPhone);

            DeliveryMethod deliveryMethod = parseDeliveryMethod(request.getParameter("deliveryMethod"));
            if (deliveryMethod == DeliveryMethod.DELIVERY) {
                InputRules.validateAddress(shippingAddress);
            }

            PaymentMethod paymentMethod = parsePaymentMethod(request.getParameter("paymentMethod"));
            if (paymentMethod == PaymentMethod.VNPAY && !VNPayConfig.isConfigured()) {
                throw new IllegalArgumentException(
                        "Hệ thống chưa cấu hình VNPay, vui lòng chọn thanh toán khi nhận hàng (COD)");
            }

            if (request.getParameter("acceptPolicy") == null) {
                throw new IllegalArgumentException("Vui lòng xác nhận chính sách đổi trả để tiếp tục");
            }

            // Lắp đặt tại cửa hàng: phải chọn ngày hẹn hợp lệ (ngày kín lịch được kiểm tra khi đặt hàng)
            LocalDate installDate = null;
            if (deliveryMethod == DeliveryMethod.INSTALL) {
                installDate = InstallationPolicy.parse(request.getParameter("installDate"));
                InstallationPolicy.validateDate(installDate, LocalDate.now());
            }

            String address = deliveryMethod == DeliveryMethod.DELIVERY ? shippingAddress.trim() : null;
            Order order = OrderDB.placeOrder(user.getAccountId(), deliveryMethod,
                    receiverName.trim(), receiverPhone.trim(), address, paymentMethod, true, installDate);

            // Tùy chọn: lưu địa chỉ giao hàng vào sổ địa chỉ (bỏ qua nếu đã có hoặc sổ đã đầy; không ảnh hưởng đơn)
            if (deliveryMethod == DeliveryMethod.DELIVERY && request.getParameter("saveAddress") != null) {
                try {
                    AddressDB.createIfAbsent(user.getAccountId(), receiverName.trim(), receiverPhone.trim(), address);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    // đơn đã đặt thành công, việc lưu địa chỉ chỉ là phụ
                }
            }

            Flash.message(request, "Đặt hàng thành công! Mã đơn hàng: " + order.getShortCode());
            String detailUrl = request.getContextPath() + "/orders/detail?id=" + order.getOrderId();

            if (paymentMethod == PaymentMethod.VNPAY) {
                // Đơn đã được giữ chỗ; chuyển khách sang cổng VNPay để trả tiền. Nếu không bắt đầu được thì khách
                // vẫn có đơn và bấm "Thanh toán lại" hoặc đổi sang COD ở trang chi tiết.
                try {
                    OnlinePayment attempt = OnlinePaymentDB.startAttempt(user.getAccountId(), order.getOrderId());
                    response.sendRedirect(VNPayUtil.buildPaymentUrl(attempt, order.getShortCode(),
                            VNPayUtil.clientIp(request)));
                    return;
                } catch (IllegalArgumentException | IllegalStateException e) {
                    Flash.error(request, e.getMessage());
                }
            }
            response.sendRedirect(detailUrl);
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
        // Sổ địa chỉ: danh sách để chọn, và địa chỉ mặc định (đứng đầu danh sách) để điền sẵn
        List<Address> addresses = AddressDB.selectByAccountId(user.getAccountId());
        request.setAttribute("addresses", addresses);
        request.setAttribute("defaultAddress",
                !addresses.isEmpty() && addresses.get(0).isDefaultAddress() ? addresses.get(0) : null);
        request.setAttribute("deliveryMethods", DeliveryMethod.values());
        // Chỉ đơn có phụ tùng mới được chọn lắp đặt tại cửa hàng
        boolean hasSparePart = false;
        for (CartLine line : summary.getLines()) {
            if (!line.isMotorbike()) {
                hasSparePart = true;
            }
        }
        request.setAttribute("hasSparePart", hasSparePart);
        request.setAttribute("laborFee", InstallationPolicy.LABOR_FEE);
        request.setAttribute("maxPerDay", InstallationPolicy.MAX_PER_DAY);
        request.setAttribute("minInstallDate", LocalDate.now().plusDays(1).toString());
        request.setAttribute("maxInstallDate", LocalDate.now().plusDays(InstallationPolicy.MAX_DAYS_AHEAD).toString());
        request.setAttribute("paymentMethods", PaymentMethod.values());
        request.setAttribute("vnpayEnabled", VNPayConfig.isConfigured());
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
