package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.NotificationDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/notifications")
public class NotificationListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("currentUser");
        String accountId = user.getAccountId();

        request.setAttribute("notifications", NotificationDB.selectByAccountId(accountId));
        request.setAttribute("unreadCount", NotificationDB.countUnread(accountId));
        Flash.consume(request);
        request.getRequestDispatcher("/WEB-INF/views/notification-list.jsp").forward(request, response);
    }
}