package com.example.algotrading.batch;

import com.example.algotrading.service.EquityBatchService;
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
public class EquityBatchScheduler {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private EquityBatchService equityBatchService;

    @Value("${kite.trade-url}")
    String kiteTradeUrl;

    @Scheduled(cron = "0 15 10 * * *", zone = "Asia/Kolkata") // 10:00 AM daily
    public void fetchAndStoreEquitiesJob() throws Exception {
        this.fetchKiteInstruments();
    }

    public void fetchKiteInstruments() throws IOException {
        String url = kiteTradeUrl + "/instruments";
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
        InputStream inputStream = new ByteArrayInputStream(Objects.requireNonNull(response.getBody()));
        equityBatchService.processCsvAndSyncEquities(inputStream);
        inputStream.close();
    }
}
