package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.dao.UserDB;
import com.example.websitemotorbike.dao.WarrantyDB;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.WarrantyRequest;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.WarrantyPolicy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@WebServlet("/admin/warranty/detail")
public class AdminWarrantyDetailServlet extends HttpServlet {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");
        WarrantyRequest warranty = (id == null || id.trim().isEmpty())
                ? null : WarrantyDB.selectById(id.trim());
        if (warranty == null) {
            response.sendRedirect(request.getContextPath() + "/admin/warranty");
            return;
        }

        Product product = ProductDB.selectById(warranty.getOrderItem().getProductId());
        String deadline = WarrantyPolicy.deadline(warranty.getOrderItem().getOrder().getCreatedAt(), product)
                .format(DATE);

        Flash.consume(request);
        request.setAttribute("warranty", warranty);
        request.setAttribute("customer", UserDB.selectByAccountId(warranty.getAccountId()));
        request.setAttribute("warrantyMonths", WarrantyPolicy.monthsFor(product));
        request.setAttribute("deadlineText", deadline);
        request.getRequestDispatcher("/WEB-INF/views/admin-warranty-detail.jsp").forward(request, response);
    }
}
