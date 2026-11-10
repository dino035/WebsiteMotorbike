package com.nguyen.motoparts.cart.controller;

import com.nguyen.motoparts.cart.dto.AddToCartRequest;
import com.nguyen.motoparts.cart.dto.CartResponse;
import com.nguyen.motoparts.cart.dto.UpdateQuantityRequest;
import com.nguyen.motoparts.cart.service.ShoppingCartService;
import com.nguyen.motoparts.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Giỏ hàng của chính người đang đăng nhập (accountId lấy từ token). Mọi thao tác đều trả lại giỏ mới nhất. */
@RestController
@RequestMapping("/api/cart")
public class ShoppingCartController {

    private final ShoppingCartService service;

    public ShoppingCartController(ShoppingCartService service) {
        this.service = service;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal AuthUser user) {
        return service.getCart(user.accountId());
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AddToCartRequest request) {
        return service.addItem(user.accountId(), request.productId(), request.quantity());
    }

    @PutMapping("/items/{cartItemId}")
    public CartResponse updateQuantity(@AuthenticationPrincipal AuthUser user, @PathVariable String cartItemId,
                                       @Valid @RequestBody UpdateQuantityRequest request) {
        return service.updateQuantity(user.accountId(), cartItemId, request.quantity());
    }

    @DeleteMapping("/items/{cartItemId}")
    public CartResponse remove(@AuthenticationPrincipal AuthUser user, @PathVariable String cartItemId) {
        return service.removeItem(user.accountId(), cartItemId);
    }
}
