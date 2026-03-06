package com.mylife.modules.user.repository;

import com.mylife.modules.user.entity.LoginActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoginActivityRepository extends JpaRepository<LoginActivity, UUID> {
    List<LoginActivity> findByUserIdOrderByLoginTimeDesc(UUID userId, org.springframework.data.domain.Pageable pageable);
}
