package com.example.algotrading.service;

import com.example.algotrading.data.entity.Equity;
import com.example.algotrading.data.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EquityBatchServiceTest {

    @InjectMocks
    private EquityBatchService equityBatchService;

    @Mock
    private EquityRepository equityRepository;
    @Mock
    private SegmentRepository segmentRepository;
    @Mock
    private ExchangeRepository exchangeRepository;
    @Mock
    private InstrumentTypeRepository instrumentTypeRepository;
    @Mock
    private EquityNameRepository equityNameRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private InputStream buildCsvInputStream(String... lines) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(baos);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("instrument_token", "exchange_token", "tradingsymbol", "name", "instrument_type", "segment", "exchange"))) {
            for (String line : lines) {
                printer.printRecord((Object[]) line.split(","));
            }
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @Test
    void test_processCsvAndSyncEquities_insertsNewEquity() throws IOException {
        // Arrange
        String[] row = {"123", "456", "INFY", "Infosys", "EQ", "NSE", "NSE"};
        InputStream csv = buildCsvInputStream(String.join(",", row));

        Mockito.when(segmentRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(exchangeRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(instrumentTypeRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(equityNameRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(equityRepository.findAll()).thenReturn(new ArrayList<>());

        // Required entity mocks
        Mockito.when(segmentRepository.save(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(exchangeRepository.save(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(instrumentTypeRepository.save(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(equityNameRepository.save(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(equityRepository.save(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        equityBatchService.processCsvAndSyncEquities(csv);

        // Assert
        Mockito.verify(equityRepository, Mockito.times(1)).save(ArgumentMatchers.any(Equity.class));
    }

    @Test
    void test_processCsvAndSyncEquities_marksEquityDeletedIfMissingInCsv() throws IOException {
        // Arrange
        String[] row = {"111", "222", "WIPRO", "Wipro", "EQ", "NSE", "NSE"};
        InputStream csv = buildCsvInputStream(String.join(",", row));

        Equity existingEquity = new Equity();
        existingEquity.setInstrumentToken("999");
        existingEquity.setDeleted(false);

        Mockito.when(segmentRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(exchangeRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(instrumentTypeRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(equityNameRepository.findAll()).thenReturn(new ArrayList<>());
        Mockito.when(equityRepository.findAll()).thenReturn(new ArrayList<>(List.of(existingEquity)));

        // Act
        equityBatchService.processCsvAndSyncEquities(csv);

        // Assert
        assertTrue(existingEquity.isDeleted());
        Mockito.verify(equityRepository, Mockito.atLeastOnce()).save(ArgumentMatchers.any());
    }

    @Test
    void test_processCsvAndSyncEquities_skipsInvalidExchange() throws IOException {
        // Arrange
        String[] row = {"222", "333", "SBIN", "SBI", "EQ", "NSE", "INVALID_EXCHANGE"};
        InputStream csv = buildCsvInputStream(String.join(",", row));

        Mockito.when(segmentRepository.findAll()).thenReturn(List.of());
        Mockito.when(exchangeRepository.findAll()).thenReturn(List.of());
        Mockito.when(instrumentTypeRepository.findAll()).thenReturn(List.of());
        Mockito.when(equityNameRepository.findAll()).thenReturn(List.of());
        Mockito.when(equityRepository.findAll()).thenReturn(List.of());

        // Act
        equityBatchService.processCsvAndSyncEquities(csv);

        // Assert
        Mockito.verify(equityRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    void test_processCsvAndSyncEquities_handlesEmptyCsv() throws IOException {
        InputStream emptyCsv = buildCsvInputStream(); // no rows

        Mockito.when(segmentRepository.findAll()).thenReturn(List.of());
        Mockito.when(exchangeRepository.findAll()).thenReturn(List.of());
        Mockito.when(instrumentTypeRepository.findAll()).thenReturn(List.of());
        Mockito.when(equityNameRepository.findAll()).thenReturn(List.of());
        Mockito.when(equityRepository.findAll()).thenReturn(List.of());

        equityBatchService.processCsvAndSyncEquities(emptyCsv);

        Mockito.verify(equityRepository, Mockito.never()).save(ArgumentMatchers.any());
    }
}