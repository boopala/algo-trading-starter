package com.example.algotrading.service;

import com.example.algotrading.data.entity.Equity;
import com.example.algotrading.data.entity.Exchange;
import com.example.algotrading.data.entity.Segment;
import com.example.algotrading.data.repository.EquityRepository;
import com.example.algotrading.data.repository.ExchangeRepository;
import com.example.algotrading.data.repository.SegmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AlgoService {

    @Autowired
    private ExchangeRepository exchangeRepository;

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private EquityRepository equityRepository;

    public List<Segment> getAllSegments() {
        String methodName = "getAllSegments ";
        log.info(methodName + "entry");
        List<Segment> segments = segmentRepository.findAll();
        log.info(methodName + "segments size: {}", segments.size());
        log.info(methodName + "exit");
        return segments;
    }

    public List<Exchange> getAllExchanges() {
        String methodName = "getAllExchanges ";
        log.info(methodName + "entry");
        List<Exchange> exchanges = exchangeRepository.findAll();
        log.info(methodName + "exchanges size: {}", exchanges.size());
        log.info(methodName + "exit");
        return exchanges;

    }

    public Page<Equity> findEquities(String segmentName, String exchangeName, String search, Pageable pageable) {
        String methodName = "findEquities ";
        log.info(methodName + "entry");
        log.debug(methodName + "segmentName {}, exchangeName {}, search {}", segmentName, exchangeName, search);
        Page<Equity> equities = null;
        Optional<Exchange> exchange = exchangeRepository.findByName(exchangeName);
        Optional<Segment> segment = segmentRepository.findByName(segmentName);
        if (segment.isPresent() && exchange.isPresent()) {
            log.debug(methodName + "segmentId {}, exchangeId {}, search {}", segment.get().getSegmentId(), exchange.get().getExchangeId(), search);
            equities = equityRepository.findBySegmentIdAndExchangeIdAndTradingSymbolContainingIgnoreCase(segment.get().getSegmentId(), exchange.get().getExchangeId(), search, pageable);
        }
        log.debug(methodName + "equities size: {}", Optional.ofNullable(equities).isEmpty() ? 0:equities.getSize());
        log.info(methodName + "exit");
        return equities;
    }

}
