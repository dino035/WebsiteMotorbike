package com.example.websitemotorbike.servlet;


import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/products/detail")
public class ProductDetailServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse respone)
        throws ServletException, IOException{

        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()){
            respone.sendRedirect(request.getContextPath() + "/products");
            return;
        }

        Product product = ProductDB.selectById(id.trim());
        if (product == null){
            respone.sendRedirect(request.getContextPath() + "/products");
            return;
        }

        request.setAttribute("product", product);
        request.getRequestDispatcher("/WEB-INF/views/product-detail.jsp").forward(request, respone);
    }
}
