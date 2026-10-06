package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.UserDB;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.model.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.util.UUID;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/register-form.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException{

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String phone = request.getParameter("phone");

        String accountId = UUID.randomUUID().toString();

        boolean exists = UserDB.emailExists(email);
        if (exists)
        {
            request.setAttribute("error", "Email đã tồn tại");
            request.getRequestDispatcher("/WEB-INF/views/register-form.jsp").forward(request, response);
        }
        else {

            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

            User user = new User(accountId, email, passwordHash, phone, UserRole.CUSTOMER);

            UserDB.insert(user);

            request.setAttribute("user", user);
            request.getRequestDispatcher("/WEB-INF/views/register-success.jsp").forward(request, response);

        }
    }

}
