package com.example.websitemotorbike.filter;

import com.example.websitemotorbike.dao.AccountLockDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Đá ra khỏi hệ thống tài khoản đang đăng nhập nhưng vừa bị Admin khóa (SRS 3.23): hủy phiên rồi chuyển
 * về /login kèm thông báo. Chỉ kiểm tra khi trong phiên có currentUser; bỏ qua tệp tĩnh (/css/).
 */
@WebFilter(urlPatterns = "/*")
public class AccountLockFilter implements Filter {

    public static final String LOCKED_MESSAGE = "Tài khoản của bạn đã bị khóa, vui lòng liên hệ cửa hàng";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!request.getServletPath().startsWith("/css/")) {
            HttpSession session = request.getSession(false);
            User currentUser = session == null ? null : (User) session.getAttribute("currentUser");

            if (currentUser != null && AccountLockDB.isLocked(currentUser.getAccountId())) {
                session.invalidate();
                Flash.error(request, LOCKED_MESSAGE);   // tạo phiên mới chỉ để mang thông báo
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
