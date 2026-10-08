package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.dao.ProductReviewDB;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import com.example.websitemotorbike.dao.VehiclePhotoDB;
import com.example.websitemotorbike.dao.SparePartPhotoDB;

@WebServlet("/products/detail")
public class ProductDetailServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }

        Product product = ProductDB.selectById(id.trim());
        if (product == null) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }

        request.setAttribute("product", product);
        if ("Motorbike".equals(product.getTypeCode())) {
            request.setAttribute("photos", VehiclePhotoDB.selectByMotorbikeId(product.getProductId()));
        } else if ("SparePart".equals(product.getTypeCode())) {
            request.setAttribute("photos", SparePartPhotoDB.selectBySparePartId(product.getProductId()));
        }

        request.setAttribute("reviews", ProductReviewDB.selectByProductId(product.getProductId()));
        request.setAttribute("averageRating", ProductReviewDB.averageRating(product.getProductId()));

        // Trang chi tiết là công khai: chỉ tính quyền đánh giá khi đã đăng nhập
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("currentUser");
        if (user != null) {
            boolean reviewed = ProductReviewDB.hasReviewed(user.getAccountId(), product.getProductId());
            request.setAttribute("alreadyReviewed", reviewed);
            request.setAttribute("canReview", !reviewed
                    && ProductReviewDB.hasPurchased(user.getAccountId(), product.getProductId()));
        }

        Flash.consume(request);
        request.getRequestDispatcher("/WEB-INF/views/product-detail.jsp").forward(request, response);
    }
}