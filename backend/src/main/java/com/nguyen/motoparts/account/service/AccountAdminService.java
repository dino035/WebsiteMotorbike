package com.nguyen.motoparts.account.service;

import com.nguyen.motoparts.account.dto.AccountRow;
import com.nguyen.motoparts.account.entity.AccountLock;
import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.AccountLockRepository;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.enums.UserRole;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Admin quản lý tài khoản khách hàng: xem, tìm, lọc, khóa, mở khóa (SRS 3.23). */
@Service
public class AccountAdminService {

    /** Đơn chưa hoàn tất: khóa tài khoản đang có đơn loại này cần admin xác nhận (force). */
    static final List<OrderStatus> UNFINISHED =
            List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING);

    private final UserRepository userRepository;
    private final AccountLockRepository lockRepository;
    private final OrderRepository orderRepository;

    public AccountAdminService(UserRepository userRepository, AccountLockRepository lockRepository,
                               OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.lockRepository = lockRepository;
        this.orderRepository = orderRepository;
    }

    /** status: ALL, ACTIVE hoặc LOCKED (giá trị lạ coi như ALL). */
    @Transactional(readOnly = true)
    public PageResponse<AccountRow> list(String keyword, String status, int page, int size) {
        Page<User> users = userRepository.searchCustomers(escapeLike(keyword), normalizeStatus(status),
                PageRequest.of(page, size, Sort.by("email")));

        List<String> ids = users.getContent().stream().map(User::getAccountId).toList();
        Map<String, AccountLock> locks = lockRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(AccountLock::getAccountId, l -> l));
        Map<String, Long> unfinished = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : orderRepository.countByAccountIds(ids, UNFINISHED)) {
                unfinished.put((String) row[0], (Long) row[1]);
            }
        }
        return PageResponse.from(users.map(u -> toRow(u, locks.get(u.getAccountId()),
                unfinished.getOrDefault(u.getAccountId(), 0L))));
    }

    /**
     * Khóa một tài khoản khách hàng. Không khóa admin. Còn đơn chưa hoàn tất thì cần force = true
     * (admin đã xác nhận sau khi xem cảnh báo, ngoại lệ 2.1 của SRS).
     */
    @Transactional
    public AccountRow lock(String adminAccountId, String accountId, String reason, boolean force) {
        User target = userRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if (target.getUserRole() != UserRole.CUSTOMER) {
            throw new BadRequestException("Chỉ khóa được tài khoản khách hàng.");
        }
        if (lockRepository.existsById(accountId)) {
            throw new ConflictException("Tài khoản này đã bị khóa.");
        }
        long unfinished = orderRepository.countByAccountIdAndStatusIn(accountId, UNFINISHED);
        if (unfinished > 0 && !force) {
            throw new ConflictException("Tài khoản đang có " + unfinished
                    + " đơn hàng chưa hoàn tất. Hãy xác nhận nếu vẫn muốn khóa.");
        }

        String cleanReason = reason == null ? "" : reason.trim();
        AccountLock lock = new AccountLock();
        lock.setAccountId(accountId);
        lock.setReason(cleanReason.isEmpty() ? null : cleanReason);
        lock.setLockedBy(adminAccountId);
        lock.setLockedAt(LocalDateTime.now());
        lockRepository.save(lock);
        return toRow(target, lock, unfinished);
    }

    /** Mở khóa: xóa dòng khóa của tài khoản. */
    @Transactional
    public AccountRow unlock(String accountId) {
        User target = userRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if (!lockRepository.existsById(accountId)) {
            throw new ConflictException("Tài khoản này chưa bị khóa.");
        }
        lockRepository.deleteById(accountId);
        return toRow(target, null, orderRepository.countByAccountIdAndStatusIn(accountId, UNFINISHED));
    }

    private static AccountRow toRow(User u, AccountLock lock, long unfinished) {
        return new AccountRow(u.getAccountId(), u.getEmail(), u.getPhone(), lock != null,
                lock == null ? null : lock.getReason(), lock == null ? null : lock.getLockedAt(), unfinished);
    }

    private static String normalizeStatus(String status) {
        return "ACTIVE".equals(status) || "LOCKED".equals(status) ? status : "ALL";
    }

    /** Thoát ký tự đặc biệt của LIKE để từ khóa như "50%" tìm đúng chữ đó. */
    private static String escapeLike(String keyword) {
        if (keyword == null) {
            return "";
        }
        return keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
