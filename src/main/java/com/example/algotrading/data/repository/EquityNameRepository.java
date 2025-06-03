package com.example.algotrading.data.repository;

import com.example.algotrading.data.entity.EquityName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquityNameRepository extends JpaRepository<EquityName, Long> {
    Optional<EquityName> findByName(String name);
}
