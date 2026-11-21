package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.cart.repository.ProductLockRepository;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.InstallationStatus;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.order.entity.InstallationRequest;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Những thứ phải trả lại khi một đơn chưa thu tiền bị hủy: xe đã bị đặt sang SOLD về AVAILABLE và chỗ trống của ngày
 * hẹn lắp đặt. Dùng chung cho Admin hủy đơn và cho việc tự hủy đơn VNPay quá hạn, để hai đường không lệch nhau.
 * Gọi trong giao dịch đang khóa dòng đơn.
 */
@Component
public class OrderStockRelease {

    private static final String MOTORBIKE = "Motorbike";

    private final ProductRepository productRepository;
    private final ProductLockRepository productLockRepository;

    public OrderStockRelease(ProductRepository productRepository, ProductLockRepository productLockRepository) {
        this.productRepository = productRepository;
        this.productLockRepository = productLockRepository;
    }

    /** Trả xe về AVAILABLE (ngoại lệ 2.1 của use case 3.21) và hủy lịch lắp đặt còn hiệu lực. */
    public void release(Order order) {
        List<OrderItem> items = order.getItems().stream().sorted(Comparator.comparing(OrderItem::getProductId)).toList();
        for (OrderItem item : items) {
            productLockRepository.lockById(item.getProductId());   // cùng thứ tự khóa với lúc đặt hàng
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null && MOTORBIKE.equals(product.getTypeCode()) && product.getStatus() == ProductStatus.SOLD) {
                product.setStatus(ProductStatus.AVAILABLE);
            }
        }
        InstallationRequest installation = order.getInstallationRequest();
        if (installation != null && installation.getStatus() == InstallationStatus.SCHEDULED) {
            installation.setStatus(InstallationStatus.CANCELLED);   // trả chỗ trống của ngày hẹn
        }
    }
}
