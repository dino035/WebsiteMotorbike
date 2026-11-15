package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.common.enums.InstallationStatus;
import com.nguyen.motoparts.order.entity.InstallationRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstallationRequestRepository extends JpaRepository<InstallationRequest, String> {

    /** Admin: mọi lịch, ngày hẹn gần nhất trước. */
    List<InstallationRequest> findAllByOrderByPreferredDateAscCreatedAtAsc();

    /** Admin: lịch theo một trạng thái, ngày hẹn gần nhất trước. */
    List<InstallationRequest> findByStatusOrderByPreferredDateAscCreatedAtAsc(InstallationStatus status);

    /** Đọc lịch và khóa dòng để hai Admin không xử lý cùng một lịch cùng lúc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from InstallationRequest r where r.requestId = :requestId")
    Optional<InstallationRequest> findForUpdate(@Param("requestId") String requestId);

    /** Số lịch còn hiệu lực (khác trạng thái) trong một ngày, dùng giới hạn số lượt mỗi ngày. */
    long countByPreferredDateAndStatusNot(LocalDate preferredDate, InstallationStatus status);

    /**
     * Khóa theo ngày (khóa tư vấn của PostgreSQL, tự nhả khi giao dịch kết thúc), để hai khách cùng đặt một ngày
     * chạy lần lượt và không làm vượt số lượt tối đa. Bọc trong SELECT để trả về một dòng bình thường.
     */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(:day) as l) t", nativeQuery = true)
    int lockDay(@Param("day") long day);
}
