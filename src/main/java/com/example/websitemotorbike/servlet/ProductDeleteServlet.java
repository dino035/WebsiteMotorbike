package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.dao.SparePartPhotoDB;
import com.example.websitemotorbike.dao.VehiclePhotoDB;
import com.example.websitemotorbike.model.SparePartPhoto;
import com.example.websitemotorbike.model.VehiclePhoto;
import com.example.websitemotorbike.util.Flash;
import com.example.websitemotorbike.util.PhotoStorage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/products/delete")
public class ProductDeleteServlet extends HttpServlet {

    // Chỉ nhận POST: mở link trực tiếp (GET) sẽ không xóa được gì
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String productId = request.getParameter("id");
        if (productId != null && !productId.trim().isEmpty()) {
            String id = productId.trim();

            // Lấy đường dẫn ảnh TRƯỚC khi xóa, vì sau khi xóa sản phẩm thì không còn dòng nào để đọc.
            // Id chỉ khớp với một loại nên một trong hai danh sách luôn rỗng.
            List<String> paths = new ArrayList<>();
            for (VehiclePhoto photo : VehiclePhotoDB.selectByMotorbikeId(id)) {
                paths.add(photo.getStoragePath());
            }
            for (SparePartPhoto photo : SparePartPhotoDB.selectBySparePartId(id)) {
                paths.add(photo.getStoragePath());
            }

            try {
                ProductDB.delete(id);
            } catch (IllegalStateException e) {
                Flash.error(request, e.getMessage());
                response.sendRedirect(request.getContextPath() + "/products");
                return;
            }

            // Xóa dòng trong DB thành công rồi mới xóa file trên Supabase
            for (String path : paths) {
                try {
                    PhotoStorage.delete(path);
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
            Flash.message(request, "Đã xóa sản phẩm");
        }

        response.sendRedirect(request.getContextPath() + "/products");
    }
}