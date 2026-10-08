package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ProductDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.websitemotorbike.dao.VehiclePhotoDB;
import com.example.websitemotorbike.model.VehiclePhoto;
import com.example.websitemotorbike.util.PhotoStorage;
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

            // Lấy danh sách ảnh TRƯỚC khi xóa, vì sau khi xóa xe thì không còn dòng nào để đọc
            List<VehiclePhoto> photos = VehiclePhotoDB.selectByMotorbikeId(id);

            ProductDB.delete(id);

            // ProductDB.delete nuốt lỗi, nên kiểm tra xe đã mất thật chưa rồi mới xóa file
            if (ProductDB.selectById(id) == null) {
                for (VehiclePhoto photo : photos) {
                    try {
                        PhotoStorage.delete(photo.getStoragePath());
                    } catch (IOException e) {
                        System.out.println(e);
                    }
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/products");
    }
}
