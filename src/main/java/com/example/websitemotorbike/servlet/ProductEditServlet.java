package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.*;
import com.example.websitemotorbike.model.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import com.example.websitemotorbike.util.Validator;
import com.example.websitemotorbike.util.PhotoStorage;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.example.websitemotorbike.util.ConditionInput;

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

        String productId = request.getParameter("id");
        Product current = (productId == null) ? null : ProductDB.selectById(productId);
        if (current == null) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }
        // Loại sản phẩm lấy từ DB, không tin giá trị gửi lên từ form
        String productType = current.getTypeCode();
        String name = request.getParameter("name");
        try {
            Validator.requireText(name, "Tên sản phẩm");
            BigDecimal price = Validator.requireDecimal(request.getParameter("price"), "Giá sản phẩm");
            ProductStatus productStatus = Validator.requireEnum(
                    request.getParameter("status"), "Trạng thái", ProductStatus.class);

            if ("Motorbike".equals(productType)) {
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

                Motorbike motorbike = (Motorbike) current;

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

                VehicleConditionAssessment assessment = motorbike.getConditionAssessment();
                if (assessment == null) {   // xe cũ chưa có đánh giá: tạo mới khi sửa
                    assessment = new VehicleConditionAssessment();
                    motorbike.setConditionAssessment(assessment);
                }
                ConditionInput.readInto(request, assessment);

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

                PartOriginType partOrigin = Validator.requireEnum(
                        request.getParameter("partOrigin"), "Nguồn gốc", PartOriginType.class);

                SparePart sparePart = (SparePart) current;

                sparePart.setName(name);
                sparePart.setPrice(price);
                sparePart.setPartType(partType);
                sparePart.setPartOrigin(partOrigin);
                sparePart.setStatus(productStatus);

                List<SparePartPhoto> existing = SparePartPhotoDB.selectBySparePartId(productId);

                // Chỉ nhận id ảnh thuộc phụ tùng này: id lạ gửi lên sẽ bị bỏ qua
                List<String> removeIds = new ArrayList<>();
                List<String> removedPaths = new ArrayList<>();
                String[] checked = request.getParameterValues("removePhoto");
                if (checked != null) {
                    Set<String> checkedSet = new HashSet<>(Arrays.asList(checked));
                    for (SparePartPhoto p : existing) {
                        if (checkedSet.contains(p.getPhotoId())) {
                            removeIds.add(p.getPhotoId());
                            removedPaths.add(p.getStoragePath());
                        }
                    }
                }

                List<Part> newParts = readNewPhotoParts(request);
                int total = existing.size() - removeIds.size() + newParts.size();
                if (total < 1) {
                    throw new IllegalArgumentException("Cần ít nhất 1 ảnh");
                }
                if (total > 10) {
                    throw new IllegalArgumentException("Tối đa 10 ảnh");
                }
                for (Part p : newParts) {
                    PhotoStorage.validate(p);
                }

                List<String> uploaded = new ArrayList<>();
                List<SparePartPhoto> toAdd = new ArrayList<>();
                try {
                    for (Part p : newParts) {
                        String path = PhotoStorage.upload(p, "spareparts");
                        uploaded.add(path);

                        SparePartPhoto photo = new SparePartPhoto();
                        photo.setStoragePath(path);
                        photo.setImageUrl(PhotoStorage.publicUrl(path));
                        toAdd.add(photo);
                    }
                    SparePartDB.update(sparePart);
                    SparePartPhotoDB.applyChanges(productId, toAdd, removeIds);
                } catch (IOException | RuntimeException e) {
                    System.out.println(e);
                    deleteQuietly(uploaded);
                    forwardWithError(request, response, productId, "Không thể lưu thay đổi, vui lòng thử lại");
                    return;
                }
                // DB đã đổi xong mới xóa file cũ trên Supabase
                deleteQuietly(removedPaths);
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
        Product product = (productId == null) ? null : ProductDB.selectById(productId);
        if (product == null) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }

        request.setAttribute("product", product);
        setPhotos(request, product);
        request.setAttribute("error", message);
        request.getRequestDispatcher("/WEB-INF/views/product-edit.jsp").forward(request, response);
    }

    private void setPhotos(HttpServletRequest request, Product product) {
        if (product == null) {
            return;
        }
        if ("Motorbike".equals(product.getTypeCode())) {
            request.setAttribute("photos", VehiclePhotoDB.selectByMotorbikeId(product.getProductId()));
        } else if ("SparePart".equals(product.getTypeCode())) {
            request.setAttribute("photos", SparePartPhotoDB.selectBySparePartId(product.getProductId()));
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
