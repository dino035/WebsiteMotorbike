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

@WebServlet("/products/edit")
public class ProductEditServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String productId = request.getParameter("id");
        Product product = ProductDB.selectById(productId);

        if (product == null){
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }
        request.setAttribute("product", product);
        request.getRequestDispatcher("/WEB-INF/views/product-edit.jsp").forward(request, response);
    }

    @Override

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{

        String productType = request.getParameter("product");
        String productId = request.getParameter("id");
        String name = request.getParameter("name");
        try {
            BigDecimal price = new BigDecimal(request.getParameter("price"));
            String productStatusStr = request.getParameter("status");
            ProductStatus productStatus = ProductStatus.valueOf(productStatusStr);

            if (productType.equals("Motorbike")){
                String brand = request.getParameter("brand");
                String model = request.getParameter("model");
                int registrationYear = Integer.parseInt(request.getParameter("registrationYear"));
                int ownerCount = Integer.parseInt(request.getParameter("ownerCount"));
                String accidentHistory = request.getParameter("accidentHistory");
                String chassisNumber = request.getParameter("chassisNumber");
                String engineNumber = request.getParameter("engineNumber");
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

                MotorbikeDB.update(motorbike);
            }

            else if(productType.equals("SparePart")){
                String partType = request.getParameter("partType");
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
        catch(IllegalArgumentException e){
            Product product = ProductDB.selectById(productId);
            request.setAttribute("product", product);
            request.setAttribute("error", "Không đúng định dạng");
            request.getRequestDispatcher("/WEB-INF/views/product-edit.jsp").forward(request, response);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/products");
    }
}
