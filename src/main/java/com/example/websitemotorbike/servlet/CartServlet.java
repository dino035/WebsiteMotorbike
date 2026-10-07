package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.CartDB;
import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.model.CartItem;
import com.example.websitemotorbike.model.CartLine;
import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.ProductStatus;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String accountId = user.getAccountId();

        List<CartLine> lines = new ArrayList<>();
        List<String> notices = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(CartDB.HOLD_MINUTES);

        for (CartItem item : CartDB.selectItems(accountId)) {
            Product product = ProductDB.selectById(item.getProductId());

            if (product == null || product.getStatus() != ProductStatus.AVAILABLE) {
                CartDB.removeItem(accountId, item.getCartItemId());
                notices.add(describe(product) + " không còn khả dụng và đã được loại khỏi giỏ hàng");
                continue;
            }
            // Xe đã hết thời hạn giữ mà khách khác đang giữ thì xe không còn là của giỏ này
            if (product instanceof Motorbike && item.getAddedAt().isBefore(cutoff)
                    && CartDB.isHeldByOther(item.getProductId(), accountId, CartDB.HOLD_MINUTES)) {
                CartDB.removeItem(accountId, item.getCartItemId());
                notices.add(describe(product) + " không còn khả dụng và đã được loại khỏi giỏ hàng");
                continue;
            }

            CartLine line = new CartLine(item, product);
            lines.add(line);
            total = total.add(line.getLineTotal());
        }

        Flash.consume(request);
        request.setAttribute("lines", lines);
        request.setAttribute("total", total);
        request.setAttribute("notices", notices);
        request.setAttribute("holdMinutes", CartDB.HOLD_MINUTES);
        request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
    }

    private String describe(Product product) {
        return product == null ? "Một sản phẩm" : "\"" + product.getName() + "\"";
    }
}
