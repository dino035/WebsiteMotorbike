package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.PasswordResetDB;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

/** Bước 2 của Reset Password (SRS 3.4): mở liên kết trong email và nhập mật khẩu mới. */
@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/reset-password.jsp";
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 72;   // giới hạn của BCrypt

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");
        if (!PasswordResetDB.isValid(token)) {
            showInvalid(request, response);
            return;
        }
        request.setAttribute("token", token.trim());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirmPassword");

        // Kiểm tra mã trước, để người dùng không điền xong mới biết liên kết đã hết hạn
        if (!PasswordResetDB.isValid(token)) {
            showInvalid(request, response);
            return;
        }

        try {
            Validator.requireText(password, "Mật khẩu mới");
            if (password.length() < MIN_LENGTH) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất " + MIN_LENGTH + " ký tự");
            }
            if (password.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("Mật khẩu mới quá dài (tối đa " + MAX_LENGTH + " ký tự)");
            }
            if (!password.equals(confirm)) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
            }

            PasswordResetDB.resetPassword(token.trim(), BCrypt.hashpw(password, BCrypt.gensalt()));

            Flash.message(request, "Đặt lại mật khẩu thành công, hãy đăng nhập bằng mật khẩu mới");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (IllegalArgumentException e) {
            if (PasswordResetDB.INVALID_LINK_MESSAGE.equals(e.getMessage())) {
                showInvalid(request, response);
                return;
            }
            // Lỗi dữ liệu: hiện lại biểu mẫu (không điền lại ô mật khẩu)
            request.setAttribute("error", e.getMessage());
            request.setAttribute("token", token.trim());
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("token", token.trim());
            request.getRequestDispatcher(VIEW).forward(request, response);
        }
    }

    private void showInvalid(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("invalid", true);
        request.setAttribute("error", PasswordResetDB.INVALID_LINK_MESSAGE);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
