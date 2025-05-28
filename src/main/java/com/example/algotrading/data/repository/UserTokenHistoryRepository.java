package com.example.algotrading.data.repository;

import com.example.algotrading.data.entity.UserTokenHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTokenHistoryRepository extends JpaRepository<UserTokenHistory, Long> {
}
