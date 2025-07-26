package com.example.algotrading.data.repository;

import com.example.algotrading.data.entity.Equity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EquityRepository extends JpaRepository<Equity, String> {
    List<Equity> findAllByIsDeletedFalse();

    Optional<Equity> findByTradingSymbol(String tradingSymbol);

    @Query("SELECT e FROM Equity e JOIN e.segment s JOIN e.exchange ex " +
            "WHERE s.id = :segmentId AND ex.id = :exchangeId AND LOWER(e.tradingSymbol) " +
            "LIKE LOWER(CONCAT('%', :search, '%')) AND e.equityName.name != '' AND e.isDeleted = false")
    Page<Equity> findBySegmentIdAndExchangeIdAndTradingSymbolContainingIgnoreCase(
            @Param("segmentId") Long segmentId,
            @Param("exchangeId") Long exchangeId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT e.instrumentToken FROM Equity e WHERE e.id = :id")
    String findInstrumentTokenById(@Param("id") Long id);

    @Query("SELECT e FROM Equity e WHERE e.instrumentToken = :instrumentToken and isDeleted = false")
    Optional<Equity> findByInstrumentToken(String instrumentToken);

    @Modifying
    @Query("UPDATE Equity e SET e.isDeleted = true WHERE e.instrumentToken IN :tokens AND e.isDeleted = false")
    int bulkMarkDeletedByInstrumentTokens(@Param("tokens") List<String> tokens);

}
