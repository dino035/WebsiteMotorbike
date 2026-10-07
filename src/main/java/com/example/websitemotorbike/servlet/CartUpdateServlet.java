package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.CartDB;
import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.model.CartItem;
import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/cart/update")
public class CartUpdateServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String cartItemId = request.getParameter("cartItemId");

        try {
            Validator.requireText(cartItemId, "Mã dòng giỏ hàng");
            Validator.requireText(request.getParameter("quantity"), "Số lượng");
            int quantity = Integer.parseInt(request.getParameter("quantity").trim());
            if (quantity < 1 || quantity > CartAddServlet.MAX_QUANTITY) {
                throw new IllegalArgumentException(
                        "Số lượng phải từ 1 đến " + CartAddServlet.MAX_QUANTITY);
            }

            CartItem item = CartDB.selectItem(user.getAccountId(), cartItemId.trim());
            if (item == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng");
            }
            Product product = ProductDB.selectById(item.getProductId());
            if (product instanceof Motorbike) {
                throw new IllegalArgumentException("Mỗi chiếc xe chỉ mua được 1 chiếc");
            }

            CartDB.updateQuantity(user.getAccountId(), item.getCartItemId(), quantity);
            Flash.message(request, "Đã cập nhật số lượng");
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
