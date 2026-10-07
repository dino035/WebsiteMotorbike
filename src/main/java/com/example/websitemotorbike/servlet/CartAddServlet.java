package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.CartDB;
import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.ProductStatus;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/cart/add")
public class CartAddServlet extends HttpServlet {

    static final int MAX_QUANTITY = 99;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String productId = request.getParameter("productId");
        String quantityText = request.getParameter("quantity");

        try {
            Validator.requireText(productId, "Mã sản phẩm");
            int quantity = (quantityText == null || quantityText.trim().isEmpty())
                    ? 1 : Integer.parseInt(quantityText.trim());
            if (quantity < 1 || quantity > MAX_QUANTITY) {
                throw new IllegalArgumentException("Số lượng phải từ 1 đến " + MAX_QUANTITY);
            }

            Product product = ProductDB.selectById(productId.trim());
            if (product == null) {
                throw new IllegalArgumentException("Sản phẩm không tồn tại");
            }
            if (product.getStatus() != ProductStatus.AVAILABLE) {
                throw new IllegalArgumentException("Sản phẩm này đã hết hàng");
            }

            boolean unique = product instanceof Motorbike;
            CartDB.AddResult result = CartDB.addItem(user.getAccountId(), product.getProductId(),
                    unique ? 1 : quantity, unique, CartDB.HOLD_MINUTES);

            if (result == CartDB.AddResult.HELD_BY_OTHER) {
                throw new IllegalArgumentException("Xe này không còn khả dụng");
            }
            Flash.message(request, "Đã thêm \"" + product.getName() + "\" vào giỏ hàng");
        } catch (NumberFormatException e) {
            Flash.error(request, "Số lượng phải là số hợp lệ");
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/cart");
    }
}
