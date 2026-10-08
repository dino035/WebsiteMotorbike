package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.MotorbikeDB;
import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.dao.SparePartDB;
import com.example.websitemotorbike.model.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import com.example.websitemotorbike.util.Validator;
import com.example.websitemotorbike.dao.VehiclePhotoDB;
import com.example.websitemotorbike.util.PhotoStorage;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebServlet("/products/edit")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 50 * 1024 * 1024)
public class ProductEditServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String productId = request.getParameter("id");
        if (productId == null || productId.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }
        Product product = ProductDB.selectById(productId);

        if (product == null){
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }
        request.setAttribute("product", product);
        setPhotos(request, product);
        request.getRequestDispatcher("/WEB-INF/views/product-edit.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            request.getParts();
        } catch (IllegalStateException e) {
            forwardWithError(request, response, request.getParameter("id"),
                    "Ảnh quá lớn, mỗi ảnh tối đa 5MB");
            return;
        }

        String productType = request.getParameter("product");
        String productId = request.getParameter("id");
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

                Motorbike motorbike = (Motorbike) ProductDB.selectById(productId);

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

                List<VehiclePhoto> existing = VehiclePhotoDB.selectByMotorbikeId(productId);

// Chỉ nhận id ảnh thuộc xe này: id lạ gửi lên sẽ bị bỏ qua
                List<String> removeIds = new ArrayList<>();
                List<String> removedPaths = new ArrayList<>();
                String[] checked = request.getParameterValues("removePhoto");
                if (checked != null) {
                    Set<String> checkedSet = new HashSet<>(Arrays.asList(checked));
                    for (VehiclePhoto p : existing) {
                        if (checkedSet.contains(p.getPhotoId())) {
                            removeIds.add(p.getPhotoId());
                            removedPaths.add(p.getStoragePath());
                        }
                    }
                }

                List<Part> newParts = readNewPhotoParts(request);
                int total = existing.size() - removeIds.size() + newParts.size();
                if (total < 1) {
                    throw new IllegalArgumentException("Xe cần ít nhất 1 ảnh");
                }
                if (total > 10) {
                    throw new IllegalArgumentException("Tối đa 10 ảnh cho mỗi xe");
                }
                for (Part p : newParts) {
                    PhotoStorage.validate(p);
                }

                List<String> uploaded = new ArrayList<>();
                List<VehiclePhoto> toAdd = new ArrayList<>();
                try {
                    for (Part p : newParts) {
                        String path = PhotoStorage.upload(p);
                        uploaded.add(path);

                        VehiclePhoto photo = new VehiclePhoto();
                        photo.setStoragePath(path);
                        photo.setImageUrl(PhotoStorage.publicUrl(path));
                        toAdd.add(photo);
                    }
                    MotorbikeDB.update(motorbike);
                    VehiclePhotoDB.applyChanges(productId, toAdd, removeIds);
                } catch (IOException | RuntimeException e) {
                    System.out.println(e);
                    deleteQuietly(uploaded);
                    forwardWithError(request, response, productId, "Không thể lưu thay đổi, vui lòng thử lại");
                    return;
                }
// DB đã đổi xong mới xóa file cũ trên Supabase
                deleteQuietly(removedPaths);
            }
            else if (productType.equals("SparePart")) {
                String partType = request.getParameter("partType");
                Validator.requireText(partType, "Loại phụ tùng");

                String partOriginStr = request.getParameter("partOrigin");
                PartOriginType partOrigin = PartOriginType.valueOf(partOriginStr);

                SparePart sparePart = (SparePart) ProductDB.selectById(productId);
                sparePart.setName(name);
                sparePart.setPrice(price);
                sparePart.setPartType(partType);
                sparePart.setPartOrigin(partOrigin);
                sparePart.setStatus(productStatus);

                SparePartDB.update(sparePart);
            }
        }
        catch (NumberFormatException e) {
            forwardWithError(request, response, productId,
                    "Giá, năm đăng ký, số đời chủ và số km đã đi phải là số hợp lệ");
            return;
        }
        catch (IllegalArgumentException e) {
            forwardWithError(request, response, productId, e.getMessage());
            return;
        }
        response.sendRedirect(request.getContextPath() + "/products");
    }

    private void forwardWithError(HttpServletRequest request, HttpServletResponse response,
                                  String productId, String message)
            throws ServletException, IOException {
        Product product = ProductDB.selectById(productId);
        request.setAttribute("product", product);
        setPhotos(request, product);
        request.setAttribute("error", message);
        request.getRequestDispatcher("/WEB-INF/views/product-edit.jsp").forward(request, response);
    }

    private void setPhotos(HttpServletRequest request, Product product) {
        if (product != null && "Motorbike".equals(product.getTypeCode())) {
            request.setAttribute("photos", VehiclePhotoDB.selectByMotorbikeId(product.getProductId()));
        }
    }

    private List<Part> readNewPhotoParts(HttpServletRequest request)
            throws ServletException, IOException {
        List<Part> result = new ArrayList<>();
        for (Part p : request.getParts()) {
            if ("photos".equals(p.getName()) && p.getSize() > 0) {
                result.add(p);
            }
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
