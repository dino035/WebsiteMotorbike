package com.nguyen.motoparts.common.enums;

// User.userRole trong đặc tả là String — enum này để dùng trong code cho an toàn kiểu,
// map sang cột String ở tầng persistence (@Enumerated(EnumType.STRING)) khi thiết kế schema.
public enum UserRole {
    CUSTOMER, ADMIN
}
