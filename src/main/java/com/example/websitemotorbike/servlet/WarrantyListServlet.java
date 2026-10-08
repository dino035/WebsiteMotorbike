package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.dao.WarrantyDB;
import com.example.websitemotorbike.model.OrderItem;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.model.WarrantyOption;
import com.example.websitemotorbike.model.WarrantyRequest;
import com.example.websitemotorbike.model.WarrantyStatus;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.WarrantyPolicy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** "Bảo hành của tôi": yêu cầu đã gửi và các sản phẩm đã mua có thể gửi yêu cầu (SRS 3.14). */
@WebServlet("/warranty")
public class WarrantyListServlet extends HttpServlet {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String accountId = user.getAccountId();

        List<WarrantyRequest> requests = WarrantyDB.selectByAccountId(accountId);
        Set<String> pendingItemIds = new HashSet<>();
        for (WarrantyRequest w : requests) {
            if (w.getStatus() == WarrantyStatus.PENDING) {
                pendingItemIds.add(w.getOrderItem().getOrderItemId());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<WarrantyOption> options = new ArrayList<>();
        for (OrderItem item : WarrantyDB.selectCompletedItems(accountId)) {
            Product product = ProductDB.selectById(item.getProductId());
            LocalDateTime purchasedAt = item.getOrder().getCreatedAt();
            LocalDateTime deadline = WarrantyPolicy.deadline(purchasedAt, product);
            options.add(new WarrantyOption(
                    item.getOrderItemId(), item.getProductName(), item.getOrder().getShortCode(),
                    purchasedAt.format(DATE), deadline.format(DATE),
                    now.isAfter(deadline), pendingItemIds.contains(item.getOrderItemId())));
        }

        Flash.consume(request);
        request.setAttribute("requests", requests);
        request.setAttribute("options", options);
        request.getRequestDispatcher("/WEB-INF/views/warranty-list.jsp").forward(request, response);
    }
}
