package com.nguyen.motoparts.compatibility.repository;

import com.nguyen.motoparts.compatibility.entity.CompatibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRule, String> {
}
