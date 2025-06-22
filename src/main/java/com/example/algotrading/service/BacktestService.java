package com.example.algotrading.service;

import com.example.algotrading.data.repository.EquityRepository;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class BacktestService {
    @Value("${kite.api-key}")
    private String apiKey;

    @Value("${kite.user-id}")
    private String userId;

    private static final int MAX_DAYS_MINUTE = 60;
    private static final int MAX_DAYS_3MINUTE = 90;
    private static final int MAX_DAYS_5MINUTE = 90;
    private static final int MAX_DAYS_10MINUTE = 90;
    private static final int MAX_DAYS_15MINUTE = 180;
    private static final int MAX_DAYS_30MINUTE = 180;
    private static final int MAX_DAYS_60MINUTE = 365;
    private static final int MAX_DAYS_HOUR = 365;
    private static final int MAX_DAYS_DAY = 2000;

    @Autowired
    private EquityRepository equityRepository;

    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private EncryptionService encryptionService;

    /*public List<HistoricalData> getHistoricalData(String equityId, String fromDateStr, String toDateStr, String interval) throws KiteException, IOException, IOException, KiteException {
        String methodName = "getHistoricalData ";
        log.info(methodName + "entry");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date fromDate;
        Date toDate;
        try {
            fromDate = formatter.parse(fromDateStr + " 09:15:00");
            toDate = formatter.parse(toDateStr + " 15:30:00");
        } catch (ParseException e) {
            log.error(methodName + "Exception occurred ", e);
            throw new RuntimeException("Invalid date format", e);
        }
        KiteConnect kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setUserId(userId);
        Optional<String> encryptedToken = userTokenService.getAccessTokenByUserId(userId);
        if (encryptedToken.isEmpty()) {
            log.info(methodName + "Access token unavailable for userId");
            throw new RuntimeException("Access Token not found");
        }
        String accessToken = encryptionService.decrypt(encryptedToken.get());
        kiteConnect.setAccessToken(accessToken);
        String instrumentToken = equityRepository.findInstrumentTokenById(Long.valueOf(equityId));
        HistoricalData historicalData = kiteConnect.getHistoricalData(fromDate, toDate, instrumentToken, interval, false, false);
        log.info(methodName + "exit");
        return historicalData.dataArrayList;
    }*/

    private int getMaxDaysForInterval(String interval) {
        switch (interval) {
            case "3minute":
                return MAX_DAYS_3MINUTE;
            case "5minute":
                return MAX_DAYS_5MINUTE;
            case "10minute":
                return MAX_DAYS_10MINUTE;
            case "15minute":
                return MAX_DAYS_15MINUTE;
            case "30minute":
                return MAX_DAYS_30MINUTE;
            case "60minute":
                return MAX_DAYS_60MINUTE;
            case "hour":
                return MAX_DAYS_HOUR;
            case "day":
                return MAX_DAYS_DAY;
            default:
                return MAX_DAYS_MINUTE;
        }
    }

    public List<HistoricalData> getHistoricalData(String equityId, String fromDateStr, String toDateStr, String interval) throws Exception, KiteException {
        String methodName = "getHistoricalData ";
        log.info(methodName + "entry");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date fromDate = formatter.parse(fromDateStr + " 09:15:00");
        Date toDate = formatter.parse(toDateStr + " 15:30:00");

        String instrumentToken = equityRepository.findInstrumentTokenById(Long.valueOf(equityId));

        int maxDays = getMaxDaysForInterval(interval);

        List<HistoricalData> allData = new ArrayList<>();
        KiteConnect kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setUserId(userId);
        Optional<String> encryptedToken = userTokenService.getAccessTokenByUserId(userId);
        if (encryptedToken.isEmpty()) {
            log.info(methodName + "Access token unavailable for userId");
            throw new RuntimeException("Access Token not found");
        }
        String accessToken = encryptionService.decrypt(encryptedToken.get());
        kiteConnect.setAccessToken(accessToken);
        Date chunkStart = fromDate;
        while (chunkStart.before(toDate)) {
            Date chunkEnd = new Date(chunkStart.getTime() + TimeUnit.DAYS.toMillis(maxDays) - 1);
            if (chunkEnd.after(toDate)) {
                chunkEnd = toDate;
            }

            HistoricalData chunkData = kiteConnect.getHistoricalData(chunkStart, chunkEnd, instrumentToken, interval, false, false);
            if (chunkData != null && chunkData.dataArrayList != null) {
                allData.addAll(chunkData.dataArrayList);
            }
            chunkStart = new Date(chunkEnd.getTime() + 1000); // move 1 second ahead to avoid overlap
        }

        // Optionally, sort allData by date if needed
        allData.sort(Comparator.comparing(d -> d.timeStamp));
        log.info(methodName + "exit");
        return allData;
    }
}
