package com.example.websitemotorbike.util;

import com.example.websitemotorbike.dao.CartDB;
import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.model.CartItem;
import com.example.websitemotorbike.model.CartLine;
import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tóm tắt giỏ hàng để hiển thị (trang giỏ hàng và trang thanh toán dùng chung).
 * Khi dựng, các dòng không còn khả dụng được tự loại khỏi giỏ và ghi vào notices.
 */
public class CartSummary {

    private final List<CartLine> lines = new ArrayList<>();
    private final List<String> notices = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;

    public static CartSummary load(String accountId) {
        CartSummary summary = new CartSummary();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(CartDB.HOLD_MINUTES);

        for (CartItem item : CartDB.selectItems(accountId)) {
            Product product = ProductDB.selectById(item.getProductId());

            if (product == null || product.getStatus() != ProductStatus.AVAILABLE) {
                CartDB.removeItem(accountId, item.getCartItemId());
                summary.notices.add(describe(product) + " không còn khả dụng và đã được loại khỏi giỏ hàng");
                continue;
            }
            // Xe đã hết thời hạn giữ mà khách khác đang giữ thì xe không còn của giỏ này
            if (product instanceof Motorbike && item.getAddedAt().isBefore(cutoff)
                    && CartDB.isHeldByOther(item.getProductId(), accountId, CartDB.HOLD_MINUTES)) {
                CartDB.removeItem(accountId, item.getCartItemId());
                summary.notices.add(describe(product) + " không còn khả dụng và đã được loại khỏi giỏ hàng");
                continue;
            }

            CartLine line = new CartLine(item, product);
            summary.lines.add(line);
            summary.total = summary.total.add(line.getLineTotal());
        }
        return summary;
    }

    private static String describe(Product product) {
        return product == null ? "Một sản phẩm" : "\"" + product.getName() + "\"";
    }

    public List<CartLine> getLines() {
        return lines;
    }

    public List<String> getNotices() {
        return notices;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
