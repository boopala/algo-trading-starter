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
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class BacktestService {
    @Value("${kite.api-key}")
    private String apiKey;

    @Value("${kite.user-id}")
    private String userId;

    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private EquityRepository equityRepository;

    public List<HistoricalData> getHistoricalData(String equityId, String fromDateStr, String toDateStr, String interval) throws KiteException, IOException, IOException, KiteException {
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
    }
}
