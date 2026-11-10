package com.nguyen.motoparts.cart.service;

import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.cart.dto.CartLineResponse;
import com.nguyen.motoparts.cart.dto.CartResponse;
import com.nguyen.motoparts.cart.entity.CartItem;
import com.nguyen.motoparts.cart.entity.ShoppingCart;
import com.nguyen.motoparts.cart.repository.CartItemRepository;
import com.nguyen.motoparts.cart.repository.ProductLockRepository;
import com.nguyen.motoparts.cart.repository.ShoppingCartRepository;
import com.nguyen.motoparts.catalog.dto.ProductSummaryResponse;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Giỏ hàng (SRS 3.10). Xe máy là hàng độc nhất: thêm vào giỏ thì được TẠM GIỮ holdMinutes phút
 * (không có bộ đếm giờ, chỉ so addedAt với thời điểm hiện tại). Hết hạn mà chưa ai khác thêm xe thì vẫn của giỏ này;
 * khi có người khác thêm và giữ, dòng của người hết hạn tự bị gỡ lần mở giỏ sau.
 *
 * Mọi thao tác khóa dòng tài khoản (SELECT ... FOR UPDATE) trước để các yêu cầu của một khách chạy lần lượt;
 * thêm xe còn khóa dòng sản phẩm để hai khách không giữ được cùng một chiếc xe. Thứ tự khóa luôn là tài khoản
 * rồi sản phẩm, nên không bị khóa chéo.
 */
@Service
public class ShoppingCartService {

    public static final int MAX_QUANTITY = 99;
    private static final String MOTORBIKE = "Motorbike";

    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final ProductLockRepository productLockRepository;
    private final UserRepository userRepository;
    private final int holdMinutes;

    public ShoppingCartService(ShoppingCartRepository cartRepository, CartItemRepository itemRepository,
                               ProductRepository productRepository, ProductLockRepository productLockRepository,
                               UserRepository userRepository,
                               @Value("${app.cart.hold-minutes:30}") int holdMinutes) {
        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
        this.productRepository = productRepository;
        this.productLockRepository = productLockRepository;
        this.userRepository = userRepository;
        this.holdMinutes = holdMinutes;
    }

    public int getHoldMinutes() {
        return holdMinutes;
    }

    /** Xem giỏ. Dòng không còn khả dụng được gỡ khỏi giỏ và ghi vào notices. */
    @Transactional
    public CartResponse getCart(String accountId) {
        lockAccount(accountId);
        return buildResponse(accountId, new ArrayList<>());
    }

    /**
     * Thêm sản phẩm vào giỏ (tạo giỏ nếu chưa có). Xe: luôn số lượng 1, tạm giữ holdMinutes phút, thêm lại thì gia hạn.
     * Phụ tùng: cộng dồn số lượng (tối đa 99).
     */
    @Transactional
    public CartResponse addItem(String accountId, String productId, Integer quantity) {
        lockAccount(accountId);
        int qty = quantity == null ? 1 : quantity;

        Product product = productRepository.findById(productId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại."));
        boolean unique = MOTORBIKE.equals(product.getTypeCode());

        if (unique) {
            productLockRepository.lockById(product.getProductId());   // hai khách cùng thêm một xe: chạy lần lượt
            product = productRepository.findById(product.getProductId()).orElseThrow();   // đọc lại sau khi khóa
        }
        if (product.getStatus() != ProductStatus.AVAILABLE) {
            throw new ConflictException("Sản phẩm này đã hết hàng.");
        }

        LocalDateTime now = LocalDateTime.now();
        ShoppingCart cart = cartRepository.findByAccountId(accountId).orElseGet(() -> {
            ShoppingCart c = new ShoppingCart();
            c.setAccountId(accountId);
            c.setCreatedAt(now);
            return cartRepository.save(c);
        });
        CartItem existing = itemRepository.findByCart_CartIdAndProductId(cart.getCartId(), product.getProductId())
                .orElse(null);

        if (unique) {
            if (itemRepository.countHeldByOthers(product.getProductId(), accountId, now.minusMinutes(holdMinutes)) > 0) {
                throw new ConflictException("Xe này đang được khách khác giữ trong giỏ hàng, vui lòng quay lại sau "
                        + holdMinutes + " phút.");
            }
            if (existing != null) {
                existing.setAddedAt(now);   // thêm lại thì gia hạn thời gian giữ
            } else {
                addNewLine(cart, product.getProductId(), 1, now);
            }
        } else if (existing != null) {
            int total = existing.getQuantity() + qty;
            if (total > MAX_QUANTITY) {
                throw new BadRequestException("Tổng số lượng một sản phẩm trong giỏ tối đa " + MAX_QUANTITY + ".");
            }
            existing.setQuantity(total);
        } else {
            addNewLine(cart, product.getProductId(), qty, now);
        }
        return buildResponse(accountId, new ArrayList<>());
    }

    /** Đặt số lượng mới cho một dòng phụ tùng. Xe máy chỉ mua được 1 chiếc. */
    @Transactional
    public CartResponse updateQuantity(String accountId, String cartItemId, int quantity) {
        lockAccount(accountId);
        CartItem item = findOwned(accountId, cartItemId);
        Product product = productRepository.findById(item.getProductId()).orElse(null);
        if (product != null && MOTORBIKE.equals(product.getTypeCode())) {
            throw new BadRequestException("Mỗi chiếc xe chỉ mua được 1 chiếc.");
        }
        item.setQuantity(quantity);
        return buildResponse(accountId, new ArrayList<>());
    }

    @Transactional
    public CartResponse removeItem(String accountId, String cartItemId) {
        lockAccount(accountId);
        CartItem item = findOwned(accountId, cartItemId);
        item.getCart().removeItem(item);   // orphanRemoval xóa dòng
        itemRepository.delete(item);
        itemRepository.flush();
        return buildResponse(accountId, new ArrayList<>());
    }

    // ----------------------------------------------------------------------------------

    private void addNewLine(ShoppingCart cart, String productId, int quantity, LocalDateTime now) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setAddedAt(now);
        cart.addItem(item);
        itemRepository.save(item);
    }

    private void lockAccount(String accountId) {
        userRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }

    private CartItem findOwned(String accountId, String cartItemId) {
        return itemRepository.findByCartItemIdAndCart_AccountId(cartItemId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng."));
    }

    /** Dựng giỏ để hiển thị, gỡ các dòng không còn khả dụng (hết hàng, hoặc xe hết hạn giữ mà người khác đang giữ). */
    private CartResponse buildResponse(String accountId, List<String> notices) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(holdMinutes);
        List<CartLineResponse> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int itemCount = 0;

        for (CartItem item : itemRepository.findByCart_AccountIdOrderByAddedAtAsc(accountId)) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);

            if (product == null || product.getStatus() != ProductStatus.AVAILABLE) {
                dropLine(item, notices, product);
                continue;
            }
            boolean motorbike = MOTORBIKE.equals(product.getTypeCode());
            if (motorbike && item.getAddedAt() != null && item.getAddedAt().isBefore(cutoff)
                    && itemRepository.countHeldByOthers(product.getProductId(), accountId, cutoff) > 0) {
                dropLine(item, notices, product);
                continue;
            }

            BigDecimal unitPrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            lines.add(new CartLineResponse(item.getCartItemId(), product.getProductId(), product.getName(),
                    product.getTypeCode(), product.getProductType(), motorbike, unitPrice, item.getQuantity(),
                    lineTotal, ProductSummaryResponse.from(product).thumbnailUrl(),
                    motorbike && item.getAddedAt() != null ? item.getAddedAt().plusMinutes(holdMinutes) : null));
            total = total.add(lineTotal);
            itemCount += item.getQuantity();
        }
        return new CartResponse(lines, total, itemCount, notices, holdMinutes);
    }

    private void dropLine(CartItem item, List<String> notices, Product product) {
        String name = product == null ? "Một sản phẩm" : "\"" + product.getName() + "\"";
        item.getCart().removeItem(item);
        itemRepository.delete(item);
        notices.add(name + " không còn khả dụng và đã được loại khỏi giỏ hàng.");
    }
}
