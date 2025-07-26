package com.example.algotrading.service;

import com.example.algotrading.data.entity.*;
import com.example.algotrading.data.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
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

    /*public void processCsvAndSyncEquities(InputStream csvInputStream) throws IOException {
        String methodName = "processCsvAndSyncEquities ";
        log.info(methodName + "entry");
        List<String> instrumentExchanges = Arrays.asList("NSE", "BSE", "BFO", "NFO", "NSEIX");
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
            log.info(methodName + "segments: {}, exchange: {}, instrumentType: {}, equityName: {}, equity: {}", segments.size(), exchanges.size(), instrumentTypes.size(), equityNames.size(), equities.size());
            List<CSVRecord> filteredRecords = parser.getRecords().stream().filter(record ->
                            instrumentExchanges.contains(record.get("exchange")))
                    .collect(Collectors.toList());
            log.info(methodName + "csv filtered records: {}", filteredRecords.size());
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
    }*/

    public void processCsvAndSyncEquities(InputStream csvInputStream) throws IOException {
        String methodName = "processCsvAndSyncEquities ";
        log.info(methodName + "entry");

        List<String> instrumentExchanges = Arrays.asList("NSE", "BSE", "BFO", "NFO", "NSEIX");
        int insertCount = 0;
        int updateCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream))) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()             // Use first record as header
                    .setSkipHeaderRecord(true)
                    .build();
            CSVParser parser = format.parse(reader);

            // Load reference data and equities once
            List<Segment> segments = segmentRepository.findAll();
            List<Exchange> exchanges = exchangeRepository.findAll();
            List<InstrumentType> instrumentTypes = instrumentTypeRepository.findAll();
            List<EquityName> equityNames = equityNameRepository.findAll();
            List<Equity> equities = equityRepository.findAllByIsDeletedFalse();

            // Map existing equities by instrument token for fast lookup/removal
            Map<String, Equity> equityMap = equities.stream()
                    .collect(Collectors.toMap(Equity::getInstrumentToken, Function.identity()));

            log.info(methodName + "segments: {}, exchange: {}, instrumentType: {}, equityName: {}, equity: {}",
                    segments.size(), exchanges.size(), instrumentTypes.size(), equityNames.size(), equities.size());

            // Filter CSV records for allowed exchanges
            List<CSVRecord> filteredRecords = parser.getRecords().stream()
                    .filter(record -> instrumentExchanges.contains(record.get("exchange")))
                    .collect(Collectors.toList());

            log.info(methodName + "csv filtered records: {}", filteredRecords.size());

            // Process each record in CSV
            for (CSVRecord record : filteredRecords) {
                String instrumentToken = record.get("instrument_token");

                if (equityMap.containsKey(instrumentToken)) {
                    // Exists: remove from map to mark as retained
                    equityMap.remove(instrumentToken);
                    // Optionally update record fields here if needed
                } else {
                    // New record, insert into DB
                    String exchangeToken = record.get("exchange_token");
                    String tradingSymbol = record.get("tradingsymbol");
                    String equityNameStr = record.get("name");
                    String instrumentTypeStr = record.get("instrument_type");
                    String segmentStr = record.get("segment");
                    String exchangeStr = record.get("exchange");
                    String expiry = record.get("expiry");

                    Equity equity = new Equity();
                    equity.setInstrumentToken(instrumentToken);
                    equity.setExchangeToken(exchangeToken);
                    equity.setTradingSymbol(tradingSymbol);
                    equity.setExpiry(expiry);

                    // Resolve Segment
                    Segment segment = segments.stream()
                            .filter(seg -> seg.getName().equals(segmentStr))
                            .findFirst()
                            .orElseGet(() -> {
                                Segment newSeg = new Segment();
                                newSeg.setName(segmentStr);
                                newSeg.setCreatedAt(LocalDateTime.now());
                                Segment savedSegment = segmentRepository.save(newSeg);
                                segments.add(savedSegment);
                                return savedSegment;
                            });
                    equity.setSegment(segment);

                    // Resolve Exchange
                    Exchange exchange = exchanges.stream()
                            .filter(ex -> ex.getName().equals(exchangeStr))
                            .findFirst()
                            .orElseGet(() -> {
                                Exchange newEx = new Exchange();
                                newEx.setName(exchangeStr);
                                newEx.setCreatedAt(LocalDateTime.now());
                                Exchange savedExchange = exchangeRepository.save(newEx);
                                exchanges.add(savedExchange);
                                return savedExchange;
                            });
                    equity.setExchange(exchange);

                    // Resolve InstrumentType
                    InstrumentType instrumentType = instrumentTypes.stream()
                            .filter(it -> it.getName().equals(instrumentTypeStr))
                            .findFirst()
                            .orElseGet(() -> {
                                InstrumentType newIT = new InstrumentType();
                                newIT.setName(instrumentTypeStr);
                                newIT.setCreatedAt(LocalDateTime.now());
                                InstrumentType savedIT = instrumentTypeRepository.save(newIT);
                                instrumentTypes.add(savedIT);
                                return savedIT;
                            });
                    equity.setInstrumentType(instrumentType);

                    // Resolve EquityName
                    EquityName equityName = equityNames.stream()
                            .filter(eq -> eq.getName().equals(equityNameStr))
                            .findFirst()
                            .orElseGet(() -> {
                                EquityName newEqName = new EquityName();
                                newEqName.setName(equityNameStr);
                                newEqName.setCreatedAt(LocalDateTime.now());
                                EquityName savedEqName = equityNameRepository.save(newEqName);
                                equityNames.add(savedEqName);
                                return savedEqName;
                            });
                    equity.setEquityName(equityName);

                    equity.setDeleted(false);

                    try {
                        equityRepository.save(equity);
                        insertCount++;
                    } catch (DataIntegrityViolationException e) {
                        // Unique constraint violation means record already exists—skip or log if needed
                        log.warn(methodName + "unique constraint violation on insert for instrument_token {}", instrumentToken);
                    }
                }
            }

            // Remaining entries in equityMap were not found in CSV => mark deleted
            List<String> tokensList = equityMap.values().stream()
                    .filter(e -> !e.isDeleted())
                    .map(Equity::getInstrumentToken).distinct().collect(Collectors.toList());
            List<List<String>> splitTokens = splitIntoChunks(tokensList);
            for (List<String> batch : splitTokens) {
                // Bulk update for each batch
                if (!batch.isEmpty()) {
                    updateCount += equityRepository.bulkMarkDeletedByInstrumentTokens(batch);
                }
            }
        }

        log.info(methodName + "insertCount: {}, updateCount: {}", insertCount, updateCount);
        log.info(methodName + "exit");
    }

    private static <T> List<List<T>> splitIntoChunks(List<T> list) {
        List<List<T>> chunks = new ArrayList<>();
        int total = list.size();
        for (int i = 0; i < total; i += 800) {
            chunks.add(list.subList(i, Math.min(total, i + 800)));
        }
        return chunks;
    }

    public void updateEquityExpiry(InputStream csvInputStream) throws IOException {
        String methodName = "updateEquityExpiry ";
        log.info(methodName + "entry");
        List<String> instrumentExchanges = Arrays.asList("NSE", "BSE", "BFO", "NFO", "NSEIX");
        AtomicInteger updateCount = new AtomicInteger();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream))) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()             // Use first record as header
                    .setSkipHeaderRecord(true)
                    .build();
            CSVParser parser = format.parse(reader);
            List<CSVRecord> filteredRecords = parser.getRecords().stream()
                    .filter(record -> {
                        return instrumentExchanges.contains(record.get("exchange")) && StringUtils.isNotBlank(record.get("expiry"));
                    })
                    .collect(Collectors.toList());
            filteredRecords.forEach(record -> {
                Optional<Equity> equity = equityRepository.findByInstrumentToken(record.get("instrument_token"));
                equity.ifPresent(value -> {
                    if (StringUtils.isBlank(equity.get().getExpiry())) {
                        value.setExpiry(record.get("expiry"));
                        equityRepository.save(value);
                        updateCount.getAndIncrement();
                        log.info(methodName + "Updated equityId: {}", value.getEquityId());
                    }
                });
            });
        }
        log.info(methodName + "totalUpdatedCount: {}", updateCount);
        log.info(methodName + "exit");
    }

}
