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

@WebServlet("/products/add")
public class ProductAddServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher("/WEB-INF/views/product-form.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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

                MotorbikeDB.insert(motorbike);
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
                    "Giá, năm đăng ký, số đời chủ và số odor phải là số hợp lệ");
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
        request.getRequestDispatcher("/WEB-INF/views/product-form.jsp").forward(request, response);
    }
}