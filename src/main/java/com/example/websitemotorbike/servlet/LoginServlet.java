package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.AccountLockDB;
import com.example.websitemotorbike.dao.UserDB;
import com.example.websitemotorbike.filter.AccountLockFilter;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String LOGIN_VIEW = "/WEB-INF/views/login.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }
        Flash.consume(request);   // thông báo "tài khoản bị khóa" khi bị đá ra từ AccountLockFilter
        request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            Validator.requireText(email, "Email");
            Validator.requireText(password, "Mật khẩu");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", "Yêu cầu nhập đầy đủ thông tin Email và Mật khẩu");
            request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
            return;
        }

        User user = UserDB.selectUser(email.trim());

        if (user == null || !passwordMatches(password, user.getPasswordHash())) {
            request.setAttribute("error", "Email hoặc mật khẩu không chính xác. Vui lòng nhập lại");
            request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
            return;
        }

        // Tài khoản bị Admin khóa thì không cho đăng nhập (SRS 3.2: tài khoản phải đang hoạt động)
        if (AccountLockDB.isLocked(user.getAccountId())) {
            request.setAttribute("error", AccountLockFilter.LOCKED_MESSAGE);
            request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
            return;
        }

        // Đổi session id sau khi đăng nhập để chống session fixation
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        request.getSession(true).setAttribute("currentUser", user);

        response.sendRedirect(request.getContextPath() + "/products");
    }

    private boolean passwordMatches(String plainPassword, String passwordHash) {
        if (passwordHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            // passwordHash trong CSDL không phải chuỗi BCrypt hợp lệ
            return false;
        }
    }
}
