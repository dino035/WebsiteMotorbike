package com.nguyen.motoparts.account.repository;

import com.nguyen.motoparts.account.entity.AccountLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountLockRepository extends JpaRepository<AccountLock, String> {
}
