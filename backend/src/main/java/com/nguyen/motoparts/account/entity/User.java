package com.nguyen.motoparts.account.entity;

import com.nguyen.motoparts.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Tài khoản (đặc tả mục Account). Admin không tách lớp riêng, chỉ phân biệt qua userRole.
 * Association 1 — 0..* tới Address: phía Address giữ khóa ngoại (Address.user).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    private String accountId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();
}
