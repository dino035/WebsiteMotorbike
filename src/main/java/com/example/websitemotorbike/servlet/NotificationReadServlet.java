package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.NotificationDB;
import com.example.websitemotorbike.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/notifications/read")
public class NotificationReadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("currentUser");
        String accountId = user.getAccountId();

        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            NotificationDB.markAllAsRead(accountId);
        } else {
            NotificationDB.markAsRead(accountId, id.trim());
        }
        response.sendRedirect(request.getContextPath() + "/notifications");
    }
}