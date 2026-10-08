package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.AddressDB;
import com.example.websitemotorbike.model.Address;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.InputRules;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Thêm mới hoặc cập nhật một địa chỉ (POST receiverName, receiverPhone, addressLine, makeDefault, addressId). */
@WebServlet("/addresses/save")
public class AddressSaveServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String addressId = request.getParameter("addressId");
        boolean editing = addressId != null && !addressId.trim().isEmpty();

        try {
            String name = request.getParameter("receiverName");
            String phone = request.getParameter("receiverPhone");
            String line = request.getParameter("addressLine");

            InputRules.validateReceiver(name, phone);
            InputRules.validateAddress(line);
            boolean makeDefault = request.getParameter("makeDefault") != null;

            if (editing) {
                AddressDB.update(user.getAccountId(), addressId.trim(), name.trim(), phone.trim(),
                        line.trim(), makeDefault);
                Flash.message(request, "Đã cập nhật địa chỉ");
            } else {
                AddressDB.create(user.getAccountId(), name.trim(), phone.trim(), line.trim(), makeDefault);
                Flash.message(request, "Đã thêm địa chỉ mới");
            }
            response.sendRedirect(request.getContextPath() + "/addresses");
        } catch (IllegalArgumentException e) {
            showError(request, response, user, editing, e.getMessage());
        } catch (IllegalStateException e) {
            showError(request, response, user, editing, e.getMessage());
        }
    }

    /** Lỗi dữ liệu: hiện lại trang với biểu mẫu giữ nguyên những gì khách đã nhập (quy ước README mục 6.2). */
    private void showError(HttpServletRequest request, HttpServletResponse response, User user,
                           boolean editing, String message) throws ServletException, IOException {
        request.setAttribute("error", message);
        if (editing) {
            Address current = AddressDB.selectOwned(user.getAccountId(), request.getParameter("addressId").trim());
            if (current == null) {
                Flash.error(request, "Không tìm thấy địa chỉ");
                response.sendRedirect(request.getContextPath() + "/addresses");
                return;
            }
            request.setAttribute("editing", current);
        }
        AddressListServlet.showPage(request, response, user);
    }
}
