package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.MotorbikeDB;
import com.example.websitemotorbike.dao.SparePartDB;
import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.PartOriginType;
import com.example.websitemotorbike.model.ProductStatus;
import com.example.websitemotorbike.model.SparePart;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import com.example.websitemotorbike.util.Validator;
import com.example.websitemotorbike.model.VehiclePhoto;
import com.example.websitemotorbike.util.PhotoStorage;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/products/add")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 50 * 1024 * 1024)
public class ProductAddServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher("/WEB-INF/views/product-add.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            request.getParts();
        } catch (IllegalStateException e) {
            forwardWithError(request, response, "Ảnh quá lớn, mỗi ảnh tối đa 5MB");
            return;
        }

        String productType = request.getParameter("product");
        String name = request.getParameter("name");

        try {
            Validator.requireText(name, "Tên sản phẩm");
            BigDecimal price = new BigDecimal(request.getParameter("price"));
            String productStatusStr = request.getParameter("status");
            ProductStatus productStatus = ProductStatus.valueOf(productStatusStr);

            if (productType.equals("Motorbike")) {
                String brand = request.getParameter("brand");
                String model = request.getParameter("model");
                String accidentHistory = request.getParameter("accidentHistory");
                String chassisNumber = request.getParameter("chassisNumber");
                String engineNumber = request.getParameter("engineNumber");

                Validator.requireText(brand, "Hãng xe");
                Validator.requireText(model, "Model");
                Validator.requireText(accidentHistory, "Lịch sử tai nạn");
                Validator.requireText(chassisNumber, "Số khung");
                Validator.requireText(engineNumber, "Số máy");

                int registrationYear = Integer.parseInt(request.getParameter("registrationYear"));
                int ownerCount = Integer.parseInt(request.getParameter("ownerCount"));
                int odometerAtIntake = Integer.parseInt(request.getParameter("odometerAtIntake"));

                Motorbike motorbike = new Motorbike();
                motorbike.setName(name);
                motorbike.setPrice(price);
                motorbike.setBrand(brand);
                motorbike.setModel(model);
                motorbike.setRegistrationYear(registrationYear);
                motorbike.setOwnerCount(ownerCount);
                motorbike.setAccidentHistory(accidentHistory);
                motorbike.setChassisNumber(chassisNumber);
                motorbike.setEngineNumber(engineNumber);
                motorbike.setOdometerAtIntake(odometerAtIntake);
                motorbike.setStatus(productStatus);

                List<Part> photoParts = readPhotoParts(request);
                for (Part p : photoParts) {
                    PhotoStorage.validate(p);
                }

                List<String> uploaded = new ArrayList<>();
                try {
                    for (Part p : photoParts) {
                        String path = PhotoStorage.upload(p);
                        uploaded.add(path);

                        VehiclePhoto photo = new VehiclePhoto();
                        photo.setStoragePath(path);
                        photo.setImageUrl(PhotoStorage.publicUrl(path));
                        motorbike.addPhoto(photo);
                    }
                    MotorbikeDB.insert(motorbike);
                } catch (IOException | RuntimeException e) {
                    System.out.println(e);
                    deleteQuietly(uploaded);
                    forwardWithError(request, response, "Không thể lưu xe và ảnh, vui lòng thử lại");
                    return;
                }
            }
            else if (productType.equals("SparePart")) {
                String partType = request.getParameter("partType");
                Validator.requireText(partType, "Loại phụ tùng");

                String partOriginStr = request.getParameter("partOrigin");
                PartOriginType partOrigin = PartOriginType.valueOf(partOriginStr);

                SparePart sparePart = new SparePart();
                sparePart.setName(name);
                sparePart.setPrice(price);
                sparePart.setPartType(partType);
                sparePart.setPartOrigin(partOrigin);
                sparePart.setStatus(productStatus);

                SparePartDB.insert(sparePart);
            }
        }
        catch (NumberFormatException e) {
            forwardWithError(request, response,
                    "Giá, năm đăng ký, số đời chủ và số km đã đi phải là số hợp lệ");
            return;
        }
        catch (IllegalArgumentException e) {
            forwardWithError(request, response, e.getMessage());
            return;
        }
        response.sendRedirect(request.getContextPath() + "/products");
    }

    private void forwardWithError(HttpServletRequest request, HttpServletResponse response,
                                  String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.getRequestDispatcher("/WEB-INF/views/product-add.jsp").forward(request, response);
    }

    private List<Part> readPhotoParts(HttpServletRequest request)
            throws ServletException, IOException {
        List<Part> result = new ArrayList<>();
        try {
            for (Part p : request.getParts()) {
                if ("photos".equals(p.getName()) && p.getSize() > 0) {
                    result.add(p);
                }
            }
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Ảnh quá lớn, mỗi ảnh tối đa 5MB");
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Xe cần ít nhất 1 ảnh");
        }
        if (result.size() > 10) {
            throw new IllegalArgumentException("Tối đa 10 ảnh cho mỗi xe");
        }
        return result;
    }

    private void deleteQuietly(List<String> storagePaths) {
        for (String path : storagePaths) {
            try {
                PhotoStorage.delete(path);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}