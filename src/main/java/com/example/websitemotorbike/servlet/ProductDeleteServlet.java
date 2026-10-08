package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.dao.SparePartPhotoDB;
import com.example.websitemotorbike.model.SparePartPhoto;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.websitemotorbike.dao.VehiclePhotoDB;
import com.example.websitemotorbike.model.VehiclePhoto;
import com.example.websitemotorbike.util.PhotoStorage;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

@WebServlet("/products/delete")
public class ProductDeleteServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
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

            ProductDB.delete(id);

            // ProductDB.delete nuốt lỗi, nên kiểm tra sản phẩm đã mất thật chưa rồi mới xóa file
            if (ProductDB.selectById(id) == null) {
                for (String path : paths) {
                    try {
                        PhotoStorage.delete(path);
                    } catch (IOException e) {
                        System.out.println(e);
                    }
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/products");
    }
}
