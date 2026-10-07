package com.example.websitemotorbike.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Thông báo "một lần": đặt vào session trước khi redirect, trang đích đọc rồi xóa.
 * Dùng cho các thao tác POST xong rồi chuyển hướng (giỏ hàng, đơn hàng...).
 */
public class Flash {

    private static final String MESSAGE = "flashMessage";
    private static final String ERROR = "flashError";

    public static void message(HttpServletRequest request, String text) {
        request.getSession().setAttribute(MESSAGE, text);
    }

    public static void error(HttpServletRequest request, String text) {
        request.getSession().setAttribute(ERROR, text);
    }

    /** Chuyển thông báo từ session sang request (để JSP hiển thị) rồi xóa khỏi session. */
    public static void consume(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Object message = session.getAttribute(MESSAGE);
        Object error = session.getAttribute(ERROR);
        if (message != null) {
            request.setAttribute("flashMessage", message);
            session.removeAttribute(MESSAGE);
        }
        if (error != null) {
            request.setAttribute("flashError", error);
            session.removeAttribute(ERROR);
        }
    }
}
