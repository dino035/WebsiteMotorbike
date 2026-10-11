package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.model.OrderStatus;
import com.example.websitemotorbike.model.ProductStatus;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Số liệu cho báo cáo của Admin (SRS 3.24). Chỉ đọc dữ liệu, không ghi gì.
 *
 * Doanh thu tính trên các đơn ở trạng thái Hoàn thành: hệ thống chỉ cho hoàn thành đơn đã thu tiền
 * (OrderDB.updateStatus), nên đơn Hoàn thành chắc chắn đã thanh toán. Đơn thuộc khoảng thời gian theo NGÀY ĐẶT.
 */
public class ReportDB {

    /** Số sản phẩm tối đa trong bảng bán chạy. */
    public static final int TOP_N = 10;

    @Getter
    public static class DayRevenue {
        private final LocalDate date;
        private int orderCount;
        private BigDecimal revenue = BigDecimal.ZERO;

        DayRevenue(LocalDate date) {
            this.date = date;
        }

        /** Ngày dạng dd/MM/yyyy để hiển thị trên JSP. */
        public String getDateText() {
            return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        void add(BigDecimal amount) {
            orderCount++;
            revenue = revenue.add(amount == null ? BigDecimal.ZERO : amount);
        }
    }

    @Getter
    public static class BestSeller {
        private final String productId;
        private final String productName;
        private final long quantity;
        private final BigDecimal revenue;

        BestSeller(String productId, String productName, long quantity, BigDecimal revenue) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.revenue = revenue;
        }
    }

    @Getter
    public static class StockRow {
        private final String label;
        private long available;
        private long sold;
        private BigDecimal availableValue = BigDecimal.ZERO;

        StockRow(String label) {
            this.label = label;
        }
    }

    /** Các đơn Hoàn thành đặt trong khoảng from..to (gồm cả hai ngày), cũ trước. */
    private static List<Order> completedOrders(EntityManager em, LocalDate from, LocalDate to) {
        TypedQuery<Order> q = em.createQuery(
                "SELECT o FROM Orders o WHERE o.status = :status AND o.createdAt >= :from AND o.createdAt < :to "
                        + "ORDER BY o.createdAt", Order.class);
        q.setParameter("status", OrderStatus.COMPLETED);
        q.setParameter("from", from.atStartOfDay());
        q.setParameter("to", to.plusDays(1).atStartOfDay());
        return q.getResultList();
    }

    /** Doanh thu từng ngày (chỉ những ngày có đơn Hoàn thành), ngày cũ trước. */
    public static List<DayRevenue> revenueByDay(LocalDate from, LocalDate to) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            Map<LocalDate, DayRevenue> days = new TreeMap<>();
            for (Order order : completedOrders(em, from, to)) {
                days.computeIfAbsent(order.getCreatedAt().toLocalDate(), DayRevenue::new)
                        .add(order.getTotalAmount());
            }
            return new ArrayList<>(days.values());
        } finally {
            em.close();
        }
    }

    /** Số đơn đặt trong khoảng from..to theo từng trạng thái (chỉ trạng thái có đơn). */
    public static Map<OrderStatus, Long> orderCountsByStatus(LocalDate from, LocalDate to) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Object[]> q = em.createQuery(
                    "SELECT o.status, COUNT(o) FROM Orders o "
                            + "WHERE o.createdAt >= :from AND o.createdAt < :to GROUP BY o.status", Object[].class);
            q.setParameter("from", from.atStartOfDay());
            q.setParameter("to", to.plusDays(1).atStartOfDay());
            Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
            for (Object[] row : q.getResultList()) {
                counts.put((OrderStatus) row[0], ((Number) row[1]).longValue());
            }
            return counts;
        } finally {
            em.close();
        }
    }

    /** Sản phẩm bán chạy: theo tổng số lượng trong các đơn Hoàn thành, nhiều nhất trước. */
    public static List<BestSeller> bestSellers(LocalDate from, LocalDate to) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Object[]> q = em.createQuery(
                    "SELECT i.productId, MAX(i.productName), SUM(i.quantity), SUM(i.unitPrice * i.quantity) "
                            + "FROM OrderItem i WHERE i.order.status = :status "
                            + "AND i.order.createdAt >= :from AND i.order.createdAt < :to "
                            + "GROUP BY i.productId "
                            + "ORDER BY SUM(i.quantity) DESC, SUM(i.unitPrice * i.quantity) DESC", Object[].class);
            q.setParameter("status", OrderStatus.COMPLETED);
            q.setParameter("from", from.atStartOfDay());
            q.setParameter("to", to.plusDays(1).atStartOfDay());
            q.setMaxResults(TOP_N);
            List<BestSeller> result = new ArrayList<>();
            for (Object[] row : q.getResultList()) {
                result.add(new BestSeller((String) row[0], (String) row[1], ((Number) row[2]).longValue(),
                        row[3] == null ? BigDecimal.ZERO : new BigDecimal(row[3].toString())));
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** Tồn kho hiện tại (không phụ thuộc khoảng thời gian): số sản phẩm còn bán, đã hết và giá trị hàng còn bán. */
    public static List<StockRow> stock() {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            List<StockRow> rows = new ArrayList<>();
            rows.add(stockOf(em, "Xe máy", "Motorbike"));
            rows.add(stockOf(em, "Phụ tùng", "SparePart"));
            return rows;
        } finally {
            em.close();
        }
    }

    private static StockRow stockOf(EntityManager em, String label, String entityName) {
        TypedQuery<Object[]> q = em.createQuery(
                "SELECT p.status, COUNT(p), SUM(p.price) FROM " + entityName + " p GROUP BY p.status",
                Object[].class);
        StockRow row = new StockRow(label);
        for (Object[] data : q.getResultList()) {
            long count = ((Number) data[1]).longValue();
            if (data[0] == ProductStatus.AVAILABLE) {
                row.available = count;
                row.availableValue = data[2] == null ? BigDecimal.ZERO : new BigDecimal(data[2].toString());
            } else {
                row.sold += count;
            }
        }
        return row;
    }
}
