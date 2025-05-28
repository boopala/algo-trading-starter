package com.example.algotrading.data.repository;

import com.example.algotrading.data.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {
}
