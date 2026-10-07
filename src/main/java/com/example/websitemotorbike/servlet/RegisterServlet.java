package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.UserDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.model.UserRole;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final String FORM_VIEW = "/WEB-INF/views/register-form.jsp";
    private static final String SUCCESS_VIEW = "/WEB-INF/views/register-success.jsp";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (request.getParameter("success") != null) {
            showSuccess(request, response);
            return;
        }
        request.getRequestDispatcher(FORM_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String phone = request.getParameter("phone");

        try {
            Validator.requireText(email, "Email");
            Validator.requireText(password, "Mật khẩu");
            Validator.requireText(phone, "Số điện thoại");

            // Chuẩn hóa email để "A@x.com" và "a@x.com" không thành hai tài khoản
            String normalizedEmail = email.trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
                throw new IllegalArgumentException("Email không hợp lệ");
            }
            if (UserDB.emailExists(normalizedEmail)) {
                throw new IllegalArgumentException("Email này đã được đăng ký");
            }

            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            User user = new User(UUID.randomUUID().toString(), normalizedEmail,
                    passwordHash, phone.trim(), UserRole.CUSTOMER);

            if (!UserDB.insert(user)) {
                throw new IllegalArgumentException("Không thể tạo tài khoản, vui lòng thử lại");
            }

            // Lưu tạm vào session để trang thành công đọc một lần (tránh gửi lại form khi F5)
            request.getSession().setAttribute("registeredUser", user);
            response.sendRedirect(request.getContextPath() + "/register?success=1");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(FORM_VIEW).forward(request, response);
        }
    }

    private void showSuccess(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("registeredUser");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }
        session.removeAttribute("registeredUser");
        request.setAttribute("user", user);
        request.getRequestDispatcher(SUCCESS_VIEW).forward(request, response);
    }
}
