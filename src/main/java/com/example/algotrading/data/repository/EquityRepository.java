package com.example.algotrading.data.repository;

import com.example.algotrading.data.entity.Equity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquityRepository extends JpaRepository<Equity, String> {
    List<Equity> findAllByIsDeletedFalse();

    Optional<Equity> findByTradingSymbol(String tradingSymbol);
}
