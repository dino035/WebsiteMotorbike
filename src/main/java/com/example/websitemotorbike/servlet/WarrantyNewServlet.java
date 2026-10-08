package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.WarrantyDB;
import com.example.websitemotorbike.model.OrderItem;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Biểu mẫu gửi yêu cầu bảo hành (SRS 3.14). */
@WebServlet("/warranty/new")
public class WarrantyNewServlet extends HttpServlet {

    private static final String FORM_VIEW = "/WEB-INF/views/warranty-new.jsp";
    private static final int MAX_DESCRIPTION = 1000;
    private static final int MAX_URL = 500;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        OrderItem item = loadItem(user, request.getParameter("itemId"));
        if (item == null) {
            Flash.error(request, "Không tìm thấy sản phẩm đã mua");
            response.sendRedirect(request.getContextPath() + "/warranty");
            return;
        }
        request.setAttribute("item", item);
        request.getRequestDispatcher(FORM_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");
        String itemId = request.getParameter("itemId");
        OrderItem item = loadItem(user, itemId);
        if (item == null) {
            Flash.error(request, "Không tìm thấy sản phẩm đã mua");
            response.sendRedirect(request.getContextPath() + "/warranty");
            return;
        }

        try {
            String description = request.getParameter("description");
            String evidenceUrl = request.getParameter("evidenceUrl");

            Validator.requireText(description, "Mô tả lỗi");
            if (description.trim().length() > MAX_DESCRIPTION) {
                throw new IllegalArgumentException("Mô tả lỗi quá dài (tối đa " + MAX_DESCRIPTION + " ký tự)");
            }
            String url = evidenceUrl == null ? "" : evidenceUrl.trim();
            if (!url.isEmpty()) {
                if (url.length() > MAX_URL) {
                    throw new IllegalArgumentException("Đường dẫn ảnh/video quá dài (tối đa " + MAX_URL + " ký tự)");
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    throw new IllegalArgumentException("Đường dẫn ảnh/video phải bắt đầu bằng http:// hoặc https://");
                }
            }

            WarrantyDB.create(user.getAccountId(), item.getOrderItemId(), description.trim(),
                    url.isEmpty() ? null : url);

            Flash.message(request, "Đã gửi yêu cầu bảo hành, vui lòng chờ cửa hàng xử lý");
            response.sendRedirect(request.getContextPath() + "/warranty");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("item", item);
            request.getRequestDispatcher(FORM_VIEW).forward(request, response);
        } catch (IllegalStateException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("item", item);
            request.getRequestDispatcher(FORM_VIEW).forward(request, response);
        }
    }

    private OrderItem loadItem(User user, String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return null;
        }
        return WarrantyDB.selectOwnedItem(user.getAccountId(), itemId.trim());
    }
}
