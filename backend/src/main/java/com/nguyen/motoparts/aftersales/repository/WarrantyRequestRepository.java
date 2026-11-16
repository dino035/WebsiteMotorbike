package com.nguyen.motoparts.aftersales.repository;

import com.nguyen.motoparts.aftersales.entity.WarrantyRequest;
import com.nguyen.motoparts.common.enums.WarrantyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRequestRepository extends JpaRepository<WarrantyRequest, String> {

    /** Yêu cầu của một khách, mới nhất trước; nạp luôn dòng đơn và đơn để hiện tên sản phẩm, mã đơn. */
    @EntityGraph(attributePaths = {"orderItem", "orderItem.order"})
    List<WarrantyRequest> findByAccountIdOrderBySubmittedAtDesc(String accountId);

    /** Chỉ trả yêu cầu nếu thuộc về accountId (chống xem yêu cầu của người khác). */
    @EntityGraph(attributePaths = {"orderItem", "orderItem.order"})
    Optional<WarrantyRequest> findByRequestIdAndAccountId(String requestId, String accountId);

    /** Có yêu cầu đang ở trạng thái này cho dòng đơn không (dùng chặn gửi trùng khi đang chờ xử lý). */
    @Query("select count(w) > 0 from WarrantyRequest w where w.orderItem.orderItemId = :orderItemId and w.status = :status")
    boolean existsForItemWithStatus(@Param("orderItemId") String orderItemId, @Param("status") WarrantyStatus status);

    /** Mã các dòng đơn của khách đang có yêu cầu chờ xử lý. */
    @Query("select w.orderItem.orderItemId from WarrantyRequest w where w.accountId = :accountId and w.status = :status")
    Collection<String> findItemIdsWithStatus(@Param("accountId") String accountId, @Param("status") WarrantyStatus status);

    /** Admin: mọi yêu cầu, mới nhất trước. */
    @EntityGraph(attributePaths = {"orderItem", "orderItem.order"})
    Page<WarrantyRequest> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    /** Admin: yêu cầu theo một trạng thái, mới nhất trước. */
    @EntityGraph(attributePaths = {"orderItem", "orderItem.order"})
    Page<WarrantyRequest> findByStatusOrderBySubmittedAtDesc(WarrantyStatus status, Pageable pageable);

    /** Đọc yêu cầu và khóa dòng để hai Admin không quyết định cùng một yêu cầu cùng lúc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WarrantyRequest w where w.requestId = :requestId")
    Optional<WarrantyRequest> findForUpdate(@Param("requestId") String requestId);
}
