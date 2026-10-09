package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.ProductStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import com.example.websitemotorbike.util.Flash;

@WebServlet("/products")
public class ProductListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String keyword = request.getParameter("keyword");
        String type = request.getParameter("type");
        String statusText = request.getParameter("status");
        String minText = request.getParameter("minPrice");
        String maxText = request.getParameter("maxPrice");

        List<Product> products;
        try {
            ProductStatus status = null;
            if (statusText != null && !statusText.trim().isEmpty()) {
                try {
                    status = ProductStatus.valueOf(statusText.trim());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Tình trạng không hợp lệ.");
                }
            }

            BigDecimal minPrice = null;
            if (minText != null && !minText.trim().isEmpty()) {
                minPrice = new BigDecimal(minText.trim());
            }
            BigDecimal maxPrice = null;
            if (maxText != null && !maxText.trim().isEmpty()) {
                maxPrice = new BigDecimal(maxText.trim());
            }
            if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
                throw new IllegalArgumentException("Khoảng giá tiền không tồn tại. Vui lòng nhập lại.");
            }

            products = ProductDB.search(keyword, type, status, minPrice, maxPrice);
        } catch (NumberFormatException e) {
            request.setAttribute("error", "Giá tiền phải là số hợp lệ.");
            products = ProductDB.selectAll();
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            products = ProductDB.selectAll();
        }

        Flash.consume(request);
        request.setAttribute("products", products);
        request.getRequestDispatcher("/WEB-INF/views/product-list.jsp").forward(request, response);
    }
}