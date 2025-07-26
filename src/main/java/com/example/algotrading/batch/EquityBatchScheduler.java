package com.example.algotrading.batch;

import com.example.algotrading.service.EquityBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@Component
@Slf4j
public class EquityBatchScheduler {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private EquityBatchService equityBatchService;

    @Value("${kite.trade-url}")
    String kiteTradeUrl;

    @Scheduled(cron = "0 15 10 * * *", zone = "Asia/Kolkata") // 10:00 AM daily
    public void fetchAndStoreEquitiesJob() throws Exception {
        String methodName = "fetchAndStoreEquitiesJob ";
        log.info(methodName + "entry");
        this.fetchKiteInstruments();
        log.info(methodName + "exit");
    }

    public void fetchKiteInstruments() throws IOException {
        String methodName = "fetchKiteInstruments ";
        log.info(methodName + "entry");
        String url = kiteTradeUrl + "/instruments";
        log.debug(methodName + "kite instruments url: {}", url);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
        InputStream inputStream = new ByteArrayInputStream(Objects.requireNonNull(response.getBody()));
        equityBatchService.processCsvAndSyncEquities(inputStream);
        inputStream.close();
        log.info(methodName + "exit");
    }

    public void updateEquityExpiry() throws IOException {
        String methodName = "updateEquityExpiry ";
        log.info(methodName + "entry");
        String url = kiteTradeUrl + "/instruments";
        log.debug(methodName + "kite instruments url: {}", url);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
        InputStream inputStream = new ByteArrayInputStream(Objects.requireNonNull(response.getBody()));
        equityBatchService.updateEquityExpiry(inputStream);
        inputStream.close();
        log.info(methodName + "exit");
    }
}
