package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.MotorbikeDB;
import com.example.websitemotorbike.dao.SparePartDB;
import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.PartOriginType;
import com.example.websitemotorbike.model.SparePart;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/products/add")
public class ProductAddServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher("/WEB-INF/views/product-form.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{

        String productType = request.getParameter("product");
        String name = request.getParameter("name");
        BigDecimal price = new BigDecimal(request.getParameter("price"));

        if (productType.equals("Motorbike")){
            String brand = request.getParameter("brand");
            String model = request.getParameter("model");
            int registrationYear = Integer.parseInt(request.getParameter("registrationYear"));
            int ownerCount = Integer.parseInt(request.getParameter("ownerCount"));
            String accidentHistory = request.getParameter("accidentHistory");
            String chassisNumber = request.getParameter("chassisNumber");
            String engineNumber = request.getParameter("engineNumber");
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

            MotorbikeDB.insert(motorbike);
        }

        else if(productType.equals("SparePart")){
            String partType = request.getParameter("partType");
            String partOriginStr = request.getParameter("partOrigin");
            PartOriginType partOrigin = PartOriginType.valueOf(partOriginStr);

            SparePart sparePart = new SparePart();
            sparePart.setName(name);
            sparePart.setPrice(price);
            sparePart.setPartType(partType);
            sparePart.setPartOrigin(partOrigin);

            SparePartDB.insert(sparePart);
        }

        response.sendRedirect(request.getContextPath() + "/products");
    }
}