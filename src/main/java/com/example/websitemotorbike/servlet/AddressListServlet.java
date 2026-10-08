package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.AddressDB;
import com.example.websitemotorbike.model.Address;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Sổ địa chỉ của khách: danh sách và biểu mẫu thêm/sửa (GET /addresses, kèm ?edit=mã để sửa một địa chỉ).
 */
@WebServlet("/addresses")
public class AddressListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = (User) request.getSession().getAttribute("currentUser");

        String editId = request.getParameter("edit");
        if (editId != null && !editId.trim().isEmpty()) {
            Address editing = AddressDB.selectOwned(user.getAccountId(), editId.trim());
            if (editing == null) {
                Flash.error(request, "Không tìm thấy địa chỉ");
                response.sendRedirect(request.getContextPath() + "/addresses");
                return;
            }
            request.setAttribute("editing", editing);
        }

        Flash.consume(request);
        showPage(request, response, user);
    }

    /** Dùng chung cho doGet và AddressSaveServlet khi biểu mẫu báo lỗi. */
    static void showPage(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        request.setAttribute("addresses", AddressDB.selectByAccountId(user.getAccountId()));
        request.setAttribute("maxAddresses", AddressDB.MAX_PER_ACCOUNT);
        request.getRequestDispatcher("/WEB-INF/views/address-list.jsp").forward(request, response);
    }
}
