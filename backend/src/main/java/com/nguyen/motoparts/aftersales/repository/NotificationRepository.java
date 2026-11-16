package com.nguyen.motoparts.aftersales.repository;

import com.nguyen.motoparts.aftersales.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /** Mới nhất trước; cùng thời điểm thì theo id để thứ tự ổn định giữa các trang. */
    @Query("select n from Notification n where n.accountId = :accountId order by n.createdAt desc, n.notificationId desc")
    Page<Notification> findMine(@Param("accountId") String accountId, Pageable pageable);

    @Query("select count(n) from Notification n where n.accountId = :accountId and n.isRead = false")
    long countUnread(@Param("accountId") String accountId);

    /** Chỉ đụng thông báo của đúng tài khoản; trả số dòng khớp (0 nghĩa là không có hoặc không phải của mình). */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.notificationId = :id and n.accountId = :accountId")
    int markRead(@Param("id") String id, @Param("accountId") String accountId);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.accountId = :accountId and n.isRead = false")
    int markAllRead(@Param("accountId") String accountId);
}
