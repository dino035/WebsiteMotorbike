package com.nguyen.motoparts.cart.repository;

import com.nguyen.motoparts.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {

    List<CartItem> findByCart_AccountIdOrderByAddedAtAsc(String accountId);

    /** Chỉ trả dòng nếu nó thuộc giỏ của accountId (chống sửa hoặc xóa giỏ của người khác). */
    Optional<CartItem> findByCartItemIdAndCart_AccountId(String cartItemId, String accountId);

    Optional<CartItem> findByCart_CartIdAndProductId(String cartId, String productId);

    /** Số khách khác (không phải accountId) đang giữ sản phẩm này, tức dòng của họ được thêm sau cutoff. */
    @Query("select count(i) from CartItem i where i.productId = :productId "
            + "and i.cart.accountId <> :accountId and i.addedAt > :cutoff")
    long countHeldByOthers(@Param("productId") String productId, @Param("accountId") String accountId,
                           @Param("cutoff") LocalDateTime cutoff);
}
