package com.nguyen.motoparts.order.entity;

import com.nguyen.motoparts.common.entity.BaseEntity;
import com.nguyen.motoparts.common.enums.DeliveryMethod;
import com.nguyen.motoparts.common.enums.InstallationStatus;
import com.nguyen.motoparts.common.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn hàng. "Order" là từ khóa SQL nên bảng tên "orders".
 * Theo đặc tả: accountId (String), orderDate, status, Composition tới OrderItem (items) và InstallationRequest.
 * Khác đặc tả: KHÔNG nối tới Address mà chụp lại người nhận và địa chỉ lúc đặt (receiverName, receiverPhone,
 * shippingAddress), để khách sửa hay xóa địa chỉ sau đó thì đơn cũ không đổi. Cần ghi chú chỗ này trong class diagram.
 * Giữ thêm từ WebsiteMotorbike: deliveryMethod, totalAmount, returnPolicyAccepted.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String orderId;

    @Column(nullable = false)
    private String accountId;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private InstallationRequest installationRequest;

    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    private String receiverName;

    private String receiverPhone;

    @Column(length = 500)
    private String shippingAddress;

    private BigDecimal totalAmount;

    /** Khách đã xác nhận đọc chính sách miễn đổi trả khi đặt hàng. */
    private boolean returnPolicyAccepted;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void attachInstallation(InstallationRequest request) {
        this.installationRequest = request;
        request.setOrder(this);
    }

    /**
     * Tính lại totalAmount từ các thành phần: tổng các dòng hàng, cộng phí công của lịch lắp đặt nếu lịch đó chưa hủy.
     * Đây là chỗ DUY NHẤT ghi totalAmount sau khi đặt hàng: tổng luôn bằng tổng các thành phần của nó, chạy lại bao nhiêu
     * lần cũng ra cùng kết quả, và đổi mức phí công sau này thì đơn cũ vẫn giữ đúng số phí đã lưu trong lịch của nó.
     */
    public void recalculateTotal() {
        BigDecimal sum = items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (installationRequest != null && installationRequest.getLaborFee() != null
                && installationRequest.getStatus() != InstallationStatus.CANCELLED) {
            sum = sum.add(installationRequest.getLaborFee());
        }
        this.totalAmount = sum;
    }

    /** Mã ngắn hiển thị cho khách (8 ký tự đầu của UUID). */
    public String getShortCode() {
        return orderId == null ? "" : orderId.substring(0, Math.min(8, orderId.length())).toUpperCase();
    }

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }
}
