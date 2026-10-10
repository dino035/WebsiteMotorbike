package com.example.websitemotorbike.servlet;

import com.example.websitemotorbike.dao.ReportDB;
import com.example.websitemotorbike.model.OrderStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Báo cáo doanh thu, sản phẩm bán chạy và tồn kho cho Admin (SRS 3.24). Được AdminFilter bảo vệ (/admin/*).
 * Phần hoạt động của trợ lý AI trong SRS chưa làm vì trợ lý AI không thuộc project web này (README).
 */
@WebServlet("/admin/reports")
public class AdminReportServlet extends HttpServlet {

    /** Khoảng thời gian mặc định khi mở trang: 30 ngày gần nhất. */
    private static final int DEFAULT_DAYS = 30;
    /** Khoảng xem tối đa, để một truy vấn không quét quá nhiều đơn. */
    private static final int MAX_DAYS = 366;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(DEFAULT_DAYS - 1);
        LocalDate to = today;
        String error = null;

        try {
            String fromParam = request.getParameter("from");
            String toParam = request.getParameter("to");
            if (fromParam != null && !fromParam.trim().isEmpty()) {
                from = LocalDate.parse(fromParam.trim());
            }
            if (toParam != null && !toParam.trim().isEmpty()) {
                to = LocalDate.parse(toParam.trim());
            }
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
            }
            if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
                throw new IllegalArgumentException("Chỉ xem báo cáo tối đa " + MAX_DAYS + " ngày mỗi lần");
            }
        } catch (DateTimeParseException e) {
            error = "Ngày không hợp lệ";
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        if (error != null) {
            // Lỗi nhập: quay về khoảng mặc định, vẫn hiện báo cáo và thông báo lỗi
            request.setAttribute("error", error);
            from = today.minusDays(DEFAULT_DAYS - 1);
            to = today;
        }

        Map<OrderStatus, Long> statusCounts = ReportDB.orderCountsByStatus(from, to);
        List<ReportDB.DayRevenue> days = ReportDB.revenueByDay(from, to);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        int completedOrders = 0;
        for (ReportDB.DayRevenue day : days) {
            totalRevenue = totalRevenue.add(day.getRevenue());
            completedOrders += day.getOrderCount();
        }
        BigDecimal average = completedOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(completedOrders), 0, RoundingMode.HALF_UP);

        request.setAttribute("from", from.toString());
        request.setAttribute("to", to.toString());
        request.setAttribute("today", today.toString());
        request.setAttribute("noData", statusCounts.isEmpty());   // không có đơn nào trong khoảng đã chọn
        request.setAttribute("statusCounts", statusCounts);
        request.setAttribute("days", days);
        request.setAttribute("totalRevenue", totalRevenue);
        request.setAttribute("completedOrders", completedOrders);
        request.setAttribute("averageOrder", average);
        request.setAttribute("bestSellers", ReportDB.bestSellers(from, to));
        request.setAttribute("topN", ReportDB.TOP_N);
        request.setAttribute("stock", ReportDB.stock());
        request.getRequestDispatcher("/WEB-INF/views/admin-report.jsp").forward(request, response);
    }
}
