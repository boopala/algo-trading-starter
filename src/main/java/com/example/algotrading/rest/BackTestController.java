package com.example.algotrading.rest;

import com.example.algotrading.model.BackTestResult;
import com.example.algotrading.model.request.BackTestRequest;
import com.example.algotrading.model.response.HistoricalData;
import com.example.algotrading.service.BackTestService;
import com.example.algotrading.service.HistoricalDataService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backtest")
@Slf4j
public class BackTestController {
    @Autowired
    private BackTestService backTestService;

    @Autowired
    private HistoricalDataService historicalDataService;

    @GetMapping("/historical-data")
    public ResponseEntity<?> getHistoricalData(
            @RequestParam String equityId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam String interval
    ) {
        String methodName= "getHistoricalData ";
        log.info(methodName + "entry");
        try {
            List<HistoricalData> data = historicalDataService.getHistoricalData(equityId, fromDate, toDate, interval);
            log.info(methodName + "exit");
            return ResponseEntity.ok(data);
        } catch (Exception | KiteException e) {
            log.error(methodName + "Exception occurred ", e);
            log.info(methodName + "exit");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/ema-rsi")
    public ResponseEntity<?> backTestEmaRsiStrategy(@RequestBody BackTestRequest request) {
        String methodName = "backTestEmaRsiStrategy ";
        log.info(methodName + "entry");
        try {
            List<HistoricalData> data = historicalDataService.getHistoricalData(
                    request.getEquityId(),
                    request.getFromDate(),
                    request.getToDate(),
                    request.getInterval()
            );

            BackTestResult result = backTestService.backTestEmaRsiStrategy(
                    data,
                    request.getEmaShortPeriod(),
                    request.getEmaLongPeriod(),
                    request.getRsiPeriod(),
                    request.getEntryRsiThreshold(),
                    request.getStopLossPercent(),
                    request.getTakeProfitPercent(),
                    request.getInterval(),
                    request.isUseTrailingStopLoss()
            );
            log.info(methodName + "exit");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error(methodName + "Exception Occurred ", e);
            log.info(methodName + "exit");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        } catch (KiteException e) {
            log.info(methodName + "exit");
            throw new RuntimeException(e);
        }
    }

}
