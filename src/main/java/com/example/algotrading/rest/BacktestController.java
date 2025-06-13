package com.example.algotrading.rest;

import com.example.algotrading.service.BacktestService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/backtest")
@Slf4j
public class BacktestController {
    @Autowired
    private BacktestService backtestService;

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
            List<HistoricalData> data = backtestService.getHistoricalData(equityId, fromDate, toDate, interval);
            log.info(methodName + "exit");
            return ResponseEntity.ok(data);
        } catch (Exception | KiteException e) {
            log.error(methodName + "Exception occurred ", e);
            log.info(methodName + "exit");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
