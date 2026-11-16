package com.nguyen.motoparts.aftersales.service;

import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.aftersales.dto.CreateWarrantyRequest;
import com.nguyen.motoparts.aftersales.dto.WarrantyOption;
import com.nguyen.motoparts.aftersales.dto.WarrantyRequestResponse;
import com.nguyen.motoparts.aftersales.entity.WarrantyRequest;
import com.nguyen.motoparts.aftersales.repository.WarrantyRequestRepository;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.NotificationType;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.enums.UserRole;
import com.nguyen.motoparts.common.enums.WarrantyStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.order.entity.OrderItem;
import com.nguyen.motoparts.order.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Bảo hành phía khách (SRS 3.14): xem sản phẩm đã mua kèm hạn bảo hành, gửi yêu cầu, xem yêu cầu của mình.
 *
 * Gửi yêu cầu cần: sản phẩm thuộc khách, đơn đã Hoàn thành, còn trong hạn (xe 2 năm, phụ tùng 6 tháng tính từ ngày đặt đơn),
 * chưa có yêu cầu đang chờ xử lý cho sản phẩm đó. Khóa dòng tài khoản (SELECT ... FOR UPDATE) trước để bấm gửi hai lần
 * cùng lúc không tạo hai yêu cầu đang chờ. Gửi xong có thông báo cho mọi Admin.
 */
@Service
public class WarrantyRequestService {

    private static final String MOTORBIKE = "Motorbike";
    private static final Pattern HTTP_URL = Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE);

    private final WarrantyRequestRepository repository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public WarrantyRequestService(WarrantyRequestRepository repository, OrderItemRepository orderItemRepository,
                                  ProductRepository productRepository, UserRepository userRepository,
                                  NotificationService notificationService) {
        this.repository = repository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /** Sản phẩm khách đã mua trong đơn Hoàn thành, mới nhất trước, kèm hạn bảo hành và có gửi yêu cầu được không. */
    @Transactional(readOnly = true)
    public List<WarrantyOption> options(String accountId) {
        Collection<String> pendingIds = repository.findItemIdsWithStatus(accountId, WarrantyStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        return orderItemRepository.findByAccountAndOrderStatus(accountId, OrderStatus.COMPLETED).stream().map(i -> {
            LocalDateTime purchasedAt = i.getOrder().getOrderDate();
            LocalDateTime deadline = WarrantyPolicy.deadline(purchasedAt, isMotorbike(i));
            boolean expired = now.isAfter(deadline);
            boolean pending = pendingIds.contains(i.getOrderItemId());
            return new WarrantyOption(i.getOrderItemId(), i.getProductName(), i.getOrder().getShortCode(),
                    purchasedAt, deadline, expired, pending, !expired && !pending);
        }).toList();
    }

    @Transactional
    public WarrantyRequestResponse create(String accountId, CreateWarrantyRequest req) {
        userRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));

        OrderItem item = orderItemRepository.findOwned(req.orderItemId().trim(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm đã mua."));
        if (item.getOrder().getStatus() != OrderStatus.COMPLETED) {
            throw new ConflictException("Chỉ gửi được yêu cầu bảo hành cho sản phẩm trong đơn hàng đã hoàn thành.");
        }
        if (!WarrantyPolicy.isWithin(item.getOrder().getOrderDate(), isMotorbike(item), LocalDateTime.now())) {
            throw new ConflictException("Sản phẩm này đã hết thời hạn bảo hành.");
        }
        if (repository.existsForItemWithStatus(item.getOrderItemId(), WarrantyStatus.PENDING)) {
            throw new ConflictException("Yêu cầu bảo hành của sản phẩm này đang chờ xử lý.");
        }

        String url = req.attachmentUrl() == null ? "" : req.attachmentUrl().trim();
        if (!url.isEmpty() && !HTTP_URL.matcher(url).matches()) {
            // Admin sẽ bấm vào đường dẫn này, nên chỉ nhận http/https (chặn javascript: và các kiểu khác)
            throw new BadRequestException("Đường dẫn ảnh/video phải bắt đầu bằng http:// hoặc https://.");
        }

        WarrantyRequest request = new WarrantyRequest();
        request.setAccountId(accountId);
        request.setOrderItem(item);
        request.setDescription(req.description().trim());
        request.setAttachmentUrl(url.isEmpty() ? null : url);
        request.setStatus(WarrantyStatus.PENDING);
        request.setSubmittedAt(LocalDateTime.now());
        repository.save(request);

        String content = "Có yêu cầu bảo hành mới #" + request.getShortCode()
                + " cho sản phẩm \"" + item.getProductName() + "\"";
        userRepository.findByUserRole(UserRole.ADMIN)
                .forEach(admin -> notificationService.notify(admin.getAccountId(), NotificationType.WARRANTY_UPDATED, content));
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public List<WarrantyRequestResponse> listMine(String accountId) {
        return repository.findByAccountIdOrderBySubmittedAtDesc(accountId).stream()
                .map(WarrantyRequestService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WarrantyRequestResponse getMine(String accountId, String requestId) {
        return repository.findByRequestIdAndAccountId(requestId, accountId)
                .map(WarrantyRequestService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu bảo hành."));
    }

    // ---------------------------------------------------------------- dùng chung với phần Admin

    /** Xe bảo hành 2 năm, còn lại (kể cả sản phẩm đã bị xóa khỏi hệ thống) 6 tháng. */
    boolean isMotorbike(OrderItem item) {
        Product product = productRepository.findById(item.getProductId()).orElse(null);
        return product != null && MOTORBIKE.equals(product.getTypeCode());
    }

    static WarrantyRequestResponse toResponse(WarrantyRequest w) {
        OrderItem item = w.getOrderItem();
        return new WarrantyRequestResponse(w.getRequestId(), w.getShortCode(), item.getOrderItemId(),
                item.getProductName(), item.getOrder().getShortCode(), w.getDescription(), w.getAttachmentUrl(),
                w.getStatus().name(), w.getStatus().getLabel(), w.getSubmittedAt(), w.getDecisionReason(), w.getResolvedAt());
    }
}
