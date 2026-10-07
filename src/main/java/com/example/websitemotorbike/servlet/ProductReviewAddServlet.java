package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.dao.ProductReviewDB;
import com.example.websitemotorbike.model.ProductReview;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/reviews/add")
public class ProductReviewAddServlet extends HttpServlet {

    static final int MAX_COMMENT_LENGTH = 1000;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String accountId = user.getAccountId();

        String productId = request.getParameter("productId");
        if (productId == null || productId.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }
        productId = productId.trim();

        String ratingText = request.getParameter("rating");
        String comment = request.getParameter("comment");

        try {
            if (ProductDB.selectById(productId) == null) {
                throw new IllegalArgumentException("Sản phẩm không tồn tại");
            }
            int rating = Integer.parseInt(ratingText == null ? "" : ratingText.trim());
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");
            }
            if (comment == null || comment.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập nội dung đánh giá");
            }
            comment = comment.trim();
            if (comment.length() > MAX_COMMENT_LENGTH) {
                throw new IllegalArgumentException("Nội dung đánh giá tối đa " + MAX_COMMENT_LENGTH + " ký tự");
            }
            if (!ProductReviewDB.hasPurchased(accountId, productId)) {
                throw new IllegalArgumentException("Bạn chỉ có thể đánh giá sản phẩm đã mua");
            }
            if (ProductReviewDB.hasReviewed(accountId, productId)) {
                throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này rồi");
            }

            ProductReview review = new ProductReview();
            review.setAccountId(accountId);
            review.setProductId(productId);
            review.setRating(rating);
            review.setComment(comment);
            ProductReviewDB.insert(review);

            Flash.message(request, "Cảm ơn bạn đã đánh giá sản phẩm");
        } catch (NumberFormatException e) {
            Flash.error(request, "Điểm đánh giá phải là số từ 1 đến 5");
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/products/detail?id="
                + URLEncoder.encode(productId, StandardCharsets.UTF_8));
    }
}