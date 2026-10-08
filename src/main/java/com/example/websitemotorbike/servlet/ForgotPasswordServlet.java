package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.PasswordResetDB;
import com.example.websitemotorbike.dao.UserDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Mailer;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Bước 1 của Reset Password (SRS 3.4): nhập email đã đăng ký rồi gửi liên kết đặt lại mật khẩu. */
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final String FORM_VIEW = "/WEB-INF/views/forgot-password.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Flash.consume(request);
        request.getRequestDispatcher(FORM_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");

        try {
            Validator.requireText(email, "Email");

            User user = UserDB.selectUser(email.trim());
            if (user == null) {
                // Đúng ngoại lệ 2.1 của SRS (lưu ý: cho biết email có đăng ký hay không)
                throw new IllegalArgumentException("Không tìm thấy email này");
            }

            // Tạo địa chỉ trước để lỗi cấu hình (thiếu app.baseUrl) không để lại mã vô chủ
            String baseUrl = Mailer.baseUrl(request);
            String token = PasswordResetDB.createToken(user.getAccountId(),
                    Mailer.resetExpireMinutes(), Mailer.resetMaxPerHour());

            String link = baseUrl + "/reset-password?token=" + token;
            String body = "Xin chào,\n\n"
                    + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản " + user.getEmail() + ".\n"
                    + "Bấm vào liên kết dưới đây để đặt mật khẩu mới (có hiệu lực trong "
                    + Mailer.resetExpireMinutes() + " phút, chỉ dùng được một lần):\n\n"
                    + link + "\n\n"
                    + "Nếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu của bạn không bị thay đổi.\n";
            try {
                Mailer.send(user.getEmail(), "Đặt lại mật khẩu - Website xe máy và phụ tùng", body);
            } catch (IllegalStateException e) {
                PasswordResetDB.discard(token);   // gửi thất bại thì bỏ mã vừa tạo
                throw e;
            }

            Flash.message(request, "Chúng tôi đã gửi liên kết đặt lại mật khẩu đến email của bạn. "
                    + "Liên kết có hiệu lực " + Mailer.resetExpireMinutes() + " phút.");
            response.sendRedirect(request.getContextPath() + "/forgot-password");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(FORM_VIEW).forward(request, response);
        } catch (IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher(FORM_VIEW).forward(request, response);
        }
    }
}
