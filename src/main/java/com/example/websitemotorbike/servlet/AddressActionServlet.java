package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.AddressDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Xóa một địa chỉ hoặc đặt làm mặc định (POST id, action = DELETE hoặc DEFAULT). */
@WebServlet("/addresses/action")
public class AddressActionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");

        try {
            String id = request.getParameter("id");
            Validator.requireText(id, "Mã địa chỉ");
            String action = request.getParameter("action");

            if ("DELETE".equals(action)) {
                AddressDB.delete(user.getAccountId(), id.trim());
                Flash.message(request, "Đã xóa địa chỉ");
            } else if ("DEFAULT".equals(action)) {
                AddressDB.setDefault(user.getAccountId(), id.trim());
                Flash.message(request, "Đã đặt làm địa chỉ mặc định");
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/addresses");
    }
}
