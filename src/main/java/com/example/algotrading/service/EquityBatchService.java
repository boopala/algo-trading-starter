package com.example.algotrading.service;

import com.example.algotrading.data.entity.*;
import com.example.algotrading.data.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EquityBatchService {

    @Autowired
    private EquityRepository equityRepository;
    @Autowired
    private SegmentRepository segmentRepository;
    @Autowired
    private ExchangeRepository exchangeRepository;
    @Autowired
    private InstrumentTypeRepository instrumentTypeRepository;
    @Autowired
    private EquityNameRepository equityNameRepository;

    public void processCsvAndSyncEquities(InputStream csvInputStream) throws IOException {
        String methodName = "processCsvAndSyncEquities ";
        log.info(methodName + "entry");
        int insertCount = 0;
        int updateCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream))) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()             // Tells the parser to use the first record as header
                    .setSkipHeaderRecord(true)
                    .build();
            CSVParser parser = format.parse(reader);

            List<Segment> segments = segmentRepository.findAll();
            List<Exchange> exchanges = exchangeRepository.findAll();
            List<InstrumentType> instrumentTypes = instrumentTypeRepository.findAll();
            List<EquityName> equityNames = equityNameRepository.findAll();
            List<Equity> equities = equityRepository.findAll();
            Set<String> instrumentTokens = equities.stream()
                    .map(Equity::getInstrumentToken)
                    .collect(Collectors.toSet());
            log.debug(methodName + "segments: {}, exchange: {}, instrumentType: {}, equityName: {}, equity: {}", segments.size(), exchanges.size(), instrumentTypes.size(), equityNames.size(), equities.size());
            List<CSVRecord> filteredRecords = parser.getRecords().stream().filter(record ->
                            record.get("exchange").startsWith("NSE") || record.get("exchange").startsWith("BSE"))
                    .collect(Collectors.toList());
            log.debug(methodName + "csv filtered records: {}", filteredRecords.size());
            for (CSVRecord record : filteredRecords) {
                String instrumentToken = record.get("instrument_token");
                if (!instrumentTokens.contains(instrumentToken)) {
                    // Not present, insert into Equity table
                    String exchangeToken = record.get("exchange_token");
                    String tradingSymbol = record.get("tradingsymbol");
                    String equityName = record.get("name");
                    String instrumentType = record.get("instrument_type");
                    String segment = record.get("segment");
                    String exchange = record.get("exchange");
                    Equity equity = new Equity();
                    equity.setInstrumentToken(instrumentToken);
                    equity.setExchangeToken(exchangeToken);
                    equity.setTradingSymbol(tradingSymbol);
                    Optional<Segment> existingSegment = segments.stream()
                            .filter(seg -> seg.getName().equals(segment))
                            .findFirst();
                    if (existingSegment.isPresent()) {
                        equity.setSegment(existingSegment.get());
                    } else {
                        Segment newSegment = new Segment();
                        newSegment.setName(segment);
                        newSegment.setCreatedAt(LocalDateTime.now());
                        newSegment = segmentRepository.save(newSegment);
                        equity.setSegment(newSegment);
                        segments.add(newSegment);
                    }

                    Optional<Exchange> existingExchange = exchanges.stream()
                            .filter(ex -> ex.getName().equals(exchange))
                            .findFirst();
                    if (existingExchange.isPresent()) {
                        equity.setExchange(existingExchange.get());
                    } else {
                        Exchange newExchange = new Exchange();
                        newExchange.setName(exchange);
                        newExchange.setCreatedAt(LocalDateTime.now());
                        newExchange = exchangeRepository.save(newExchange);
                        equity.setExchange(newExchange);
                        exchanges.add(newExchange);
                    }

                    Optional<InstrumentType> existingInstrumentType = instrumentTypes.stream()
                            .filter(it -> it.getName().equals(instrumentType))
                            .findFirst();
                    if (existingInstrumentType.isPresent()) {
                        equity.setInstrumentType(existingInstrumentType.get());
                    } else {
                        InstrumentType newInstrumentType = new InstrumentType();
                        newInstrumentType.setName(instrumentType);
                        newInstrumentType.setCreatedAt(LocalDateTime.now());
                        newInstrumentType = instrumentTypeRepository.save(newInstrumentType);
                        equity.setInstrumentType(newInstrumentType);
                        instrumentTypes.add(newInstrumentType);
                    }

                    Optional<EquityName> existingEquityName = equityNames.stream()
                            .filter(eq -> eq.getName().equals(equityName))
                            .findFirst();
                    if (existingEquityName.isPresent()) {
                        equity.setEquityName(existingEquityName.get());
                    } else {
                        EquityName newEquityName = new EquityName();
                        newEquityName.setName(equityName);
                        newEquityName.setCreatedAt(LocalDateTime.now());
                        newEquityName = equityNameRepository.save(newEquityName);
                        equity.setEquityName(newEquityName);
                        equityNames.add(newEquityName);
                    }
                    equity.setDeleted(false);
                    equityRepository.save(equity);
                    insertCount += 1;
                }

            }

            List<Equity> afterJobEquities = equityRepository.findAll();
            Set<String> excelInstrumentTokens = filteredRecords.stream()
                    .map(record->record.get("instrument_token"))
                    .collect(Collectors.toSet());
            for (Equity e : afterJobEquities) {
                if (!excelInstrumentTokens.contains(e.getInstrumentToken())) {
                    e.setDeleted(true);
                    equityRepository.save(e);
                    updateCount += 1;
                }
            }

        }
        log.info(methodName + "insertCount: {}, updateCount: {}", insertCount, updateCount);
        log.info(methodName + "exit");
    }

}
