package com.example.websitemotorbike.model;

/**
 * Một dòng trên trang quản lý tài khoản: khách hàng kèm trạng thái khóa và số đơn chưa hoàn tất.
 * Không phải entity, chỉ dùng cho view.
 */
public class AccountRow {

    private final User user;
    private final AccountLock lock;
    private final long unfinishedOrders;

    public AccountRow(User user, AccountLock lock, long unfinishedOrders) {
        this.user = user;
        this.lock = lock;
        this.unfinishedOrders = unfinishedOrders;
    }

    public User getUser() {
        return user;
    }

    /** Thông tin khóa; null nếu tài khoản đang hoạt động. */
    public AccountLock getLock() {
        return lock;
    }

    public boolean isLocked() {
        return lock != null;
    }

    /** Số đơn đang ở Chờ xác nhận, Đã xác nhận hoặc Đang giao/Chờ lấy. */
    public long getUnfinishedOrders() {
        return unfinishedOrders;
    }
}
