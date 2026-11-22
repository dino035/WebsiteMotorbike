package com.nguyen.motoparts.report.service;

import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.order.repository.OrderItemRepository;
import com.nguyen.motoparts.order.repository.OrderRepository;
import com.nguyen.motoparts.report.dto.DailyRevenueRow;
import com.nguyen.motoparts.report.dto.InventoryRow;
import com.nguyen.motoparts.report.dto.SalesReportResponse;
import com.nguyen.motoparts.report.dto.TopProductRow;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Báo cáo doanh thu cho Admin (UC-23, QĐ09): doanh thu theo ngày, 10 sản phẩm bán chạy nhất và số lượng sản phẩm
 * theo loại / trạng thái, trong khoảng ngày Admin chọn.
 *
 * Chỉ tính đơn COMPLETED. Lọc theo orderDate (ngày đặt, dữ liệu nghiệp vụ) chứ không theo createdAt — createdAt là
 * mốc kỹ thuật, còn updatedAt thì bị ghi đè mỗi lần đơn bị sửa nên không dùng làm "ngày hoàn thành" được.
 * Toàn bộ chỉ đọc, không sửa dữ liệu nào.
 */
@Service
@Transactional(readOnly = true)
public class SalesReportService {

    private static final int MAX_DAYS = 366;      // QĐ09
    private static final int DEFAULT_DAYS = 30;   // QĐ09
    private static final int TOP_SIZE = 10;       // QĐ09

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public SalesReportService(OrderRepository orderRepository,
                              OrderItemRepository orderItemRepository,
                              ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    public SalesReportResponse sales(LocalDate from, LocalDate to) {
        LocalDate end = (to == null) ? LocalDate.now() : to;
        LocalDate start = (from == null) ? end.minusDays(DEFAULT_DAYS - 1L) : from;

        if (start.isAfter(end)) {
            throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new BadRequestException("Khoảng xem tối đa " + MAX_DAYS + " ngày.");
        }

        // Nửa mở: lấy từ 00:00:00 ngày bắt đầu đến trước 00:00:00 của ngày sau ngày kết thúc, để không rơi đơn
        // đặt vào những phần giây cuối ngày.
        LocalDateTime fromTime = start.atStartOfDay();
        LocalDateTime toTime = end.plusDays(1).atStartOfDay();

        List<DailyRevenueRow> daily =
                daily(start, end, orderRepository.findRevenueRows(OrderStatus.COMPLETED, fromTime, toTime));

        long orderCount = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (DailyRevenueRow row : daily) {
            orderCount += row.orderCount();
            totalRevenue = totalRevenue.add(row.revenue());
        }

        return new SalesReportResponse(start, end, orderCount, totalRevenue, daily,
                topProducts(fromTime, toTime), inventory());
    }

    /**
     * Gộp các đơn theo ngày. Duyệt hết khoảng ngày chứ không duyệt kết quả truy vấn, để ngày không có đơn vẫn có
     * một dòng giá trị 0 — nếu không biểu đồ sẽ nhảy ngày và người xem tưởng hệ thống mất dữ liệu.
     */
    private List<DailyRevenueRow> daily(LocalDate from, LocalDate to, List<Object[]> rows) {
        Map<LocalDate, Long> counts = new HashMap<>();
        Map<LocalDate, BigDecimal> sums = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = ((LocalDateTime) row[0]).toLocalDate();
            counts.merge(date, 1L, Long::sum);
            // totalAmount không có nullable = false nên đơn cũ có thể trống.
            sums.merge(date, row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1], BigDecimal::add);
        }

        List<DailyRevenueRow> daily = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            daily.add(new DailyRevenueRow(date,
                    counts.getOrDefault(date, 0L),
                    sums.getOrDefault(date, BigDecimal.ZERO)));
        }
        return daily;
    }

    /** 10 sản phẩm bán nhiều nhất. Tên lấy từ bản chụp trong OrderItem nên sản phẩm đã xóa vẫn hiện đúng tên lúc bán. */
    private List<TopProductRow> topProducts(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderItemRepository.findTopProducts(
                OrderStatus.COMPLETED, from, to, PageRequest.of(0, TOP_SIZE));

        List<TopProductRow> top = new ArrayList<>();
        for (Object[] row : rows) {
            top.add(new TopProductRow(
                    (String) row[0],
                    (String) row[1],
                    (Long) row[2],
                    row[3] == null ? BigDecimal.ZERO : (BigDecimal) row[3]));
        }
        return top;
    }

    /**
     * Số sản phẩm theo loại và trạng thái tại thời điểm xem, không theo khoảng ngày: hệ thống không lưu lịch sử
     * tồn kho nên không dựng lại được tình trạng của quá khứ.
     */
    private List<InventoryRow> inventory() {
        List<InventoryRow> inventory = new ArrayList<>();
        for (Object[] row : productRepository.countByTypeAndStatus()) {
            // Nhãn viết tay vì truy vấn trả về lớp chứ không trả về entity; phải khớp với Motorbike.getProductType()
            // và SparePart.getProductType().
            String typeLabel = Motorbike.class.equals(row[0]) ? "Xe máy" : "Phụ tùng";
            ProductStatus status = (ProductStatus) row[1];
            inventory.add(new InventoryRow(typeLabel, status.getLabel(), (Long) row[2]));
        }
        inventory.sort(Comparator.comparing(InventoryRow::typeLabel).thenComparing(InventoryRow::statusLabel));
        return inventory;
    }
}