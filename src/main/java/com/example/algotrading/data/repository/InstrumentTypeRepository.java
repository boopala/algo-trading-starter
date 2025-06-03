package com.example.algotrading.data.repository;

import com.example.algotrading.data.entity.InstrumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstrumentTypeRepository extends JpaRepository<InstrumentType, Long> {
    Optional<InstrumentType> findByName(String name);
}
