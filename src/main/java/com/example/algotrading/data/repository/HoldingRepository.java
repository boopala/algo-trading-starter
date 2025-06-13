package com.example.algotrading.data.repository;

import com.example.algotrading.data.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findAllByIsDeletedFalse();
    Optional<Holding> findByInstrumentTokenAndIsDeletedFalse(String instrumentToken);

    Optional<Holding> findByIsinAndIsDeletedFalse(String isin);
}
