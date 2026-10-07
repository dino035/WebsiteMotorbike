package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.CartDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.CartSummary;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        CartSummary summary = CartSummary.load(user.getAccountId());

        Flash.consume(request);
        request.setAttribute("lines", summary.getLines());
        request.setAttribute("total", summary.getTotal());
        request.setAttribute("notices", summary.getNotices());
        request.setAttribute("holdMinutes", CartDB.HOLD_MINUTES);
        request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
    }
}
