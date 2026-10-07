package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.CartDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/cart/remove")
public class CartRemoveServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String cartItemId = request.getParameter("cartItemId");

        try {
            Validator.requireText(cartItemId, "Mã dòng giỏ hàng");
            if (CartDB.removeItem(user.getAccountId(), cartItemId.trim())) {
                Flash.message(request, "Đã xóa sản phẩm khỏi giỏ hàng");
            } else {
                Flash.error(request, "Không tìm thấy sản phẩm trong giỏ hàng");
            }
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/cart");
    }
}
