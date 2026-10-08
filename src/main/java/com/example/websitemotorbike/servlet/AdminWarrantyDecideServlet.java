package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.WarrantyDB;
import com.example.websitemotorbike.model.WarrantyRequest;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.Validator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin duyệt (thay mới) hoặc từ chối yêu cầu bảo hành (POST id, decision, note). */
@WebServlet("/admin/warranty/decide")
public class AdminWarrantyDecideServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");

        try {
            Validator.requireText(id, "Mã yêu cầu bảo hành");
            boolean approve = parseDecision(request.getParameter("decision"));

            WarrantyRequest warranty = WarrantyDB.decide(id.trim(), approve, request.getParameter("note"));
            Flash.message(request, "Đã " + (approve ? "duyệt" : "từ chối") + " yêu cầu bảo hành #"
                    + warranty.getShortCode());
        } catch (IllegalArgumentException e) {
            Flash.error(request, e.getMessage());
        } catch (IllegalStateException e) {
            Flash.error(request, e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/warranty/detail?id="
                + (id == null ? "" : id.trim()));
    }

    private boolean parseDecision(String value) {
        if ("APPROVE".equals(value)) {
            return true;
        }
        if ("REJECT".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("Quyết định không hợp lệ");
    }
}
