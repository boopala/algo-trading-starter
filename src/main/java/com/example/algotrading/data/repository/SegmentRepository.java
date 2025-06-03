package com.example.algotrading.data.repository;

import com.example.algotrading.data.entity.Segment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SegmentRepository extends JpaRepository<Segment, Long> {
    Optional<Segment> findByName(String name);
}
