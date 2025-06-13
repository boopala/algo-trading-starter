package com.example.algotrading.service;

import com.example.algotrading.model.response.*;
import com.example.algotrading.util.CommonUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import com.zerodhatech.models.Position;
import com.zerodhatech.models.Profile;
import com.zerodhatech.models.User;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KiteService {
    @Value("${kite.api-key}")
    private String apiKey;

    @Value("${kite.api-secret}")
    private String apiSecret;

    @Value("${kite.login-url}")
    String kiteLoginUrl;

    @Autowired
    UserTokenService userTokenService;

    @Autowired
    HoldingsService holdingsService;

    @Autowired
    ModelMapper modelMapper;

    public String generateLoginUrl() {
        String methodName = "generateLoginUrl ";
        log.info(methodName + "entry");
        String loginUrl = kiteLoginUrl + apiKey;
        log.debug(methodName + "kiteLoginUrl: {}", kiteLoginUrl);
        log.info(methodName + "exit");
        return loginUrl;
    }

    public TokenResponse generateAccessToken(String requestToken) throws Exception, KiteException {
        String methodName = "generateAccessToken ";
        log.info(methodName + "entry");
        KiteConnect kiteConnect = new KiteConnect(apiKey);
        User user = kiteConnect.generateSession(requestToken, apiSecret);
        kiteConnect.setUserId(user.userId);

        kiteConnect.setAccessToken(user.accessToken);
        kiteConnect.setPublicToken(user.publicToken);
        userTokenService.saveOrUpdateToken(user.userId, user.accessToken);

        TokenResponse response = new TokenResponse();
        response.setAccessToken(user.accessToken);
        response.setPublicToken(user.publicToken);
        response.setUserId(user.userId);
        log.info(methodName + "exit");
        return response;
    }

    public Profile getUserProfile(String accessToken, String userId) throws IOException, KiteException {
        String methodName = "getUserProfile ";
        log.info(methodName + "entry");
        KiteConnect kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setUserId(userId);
        kiteConnect.setAccessToken(accessToken);
        log.info(methodName + "exit");
        return kiteConnect.getProfile();
    }

    public List<com.example.algotrading.model.Holding> getHoldings(String accessToken, String userId) throws IOException, KiteException {
        String methodName = "getHoldings ";
        log.info(methodName + "entry");
        KiteConnect kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setUserId(userId);
        kiteConnect.setAccessToken(accessToken);
        List<Holding> holdings = kiteConnect.getHoldings();
        List<com.example.algotrading.data.entity.Holding> holdingEntities = holdingsService.saveKiteHoldings(holdings);
        if (holdingEntities.size() != holdings.size()) {
            Set<String> activeKeys = holdings.stream()
                    .map(h -> h.isin)
                    .collect(Collectors.toSet());
            for (com.example.algotrading.data.entity.Holding dbHolding : holdingEntities) {
                if (!activeKeys.contains(dbHolding.getIsin())) {
                    dbHolding.setIsDeleted(true);
                    dbHolding.setUpdatedAt(LocalDateTime.now());
                    // Optionally update updatedAt or other audit fields
                    holdingsService.updateHolding(dbHolding);
                }
            }
        }
        List<com.example.algotrading.model.Holding> holdingList = new ArrayList<>();
        holdingEntities.forEach(holdingEntity -> {
            com.example.algotrading.model.Holding holding = CommonUtil.convertHoldingEntityToDto(holdingEntity, modelMapper);
            holdingList.add(holding);
        });
        log.debug(methodName + "holdings size: {}", holdings.size());
        log.debug(methodName + "holdingsDTO size: {}", holdingList.size());
        log.info(methodName + "exit");
        return holdingList;
    }

    public Map<String, List<Position>> getPositions(String accessToken, String userId) throws IOException, KiteException {
        String methodName = "getPositions ";
        log.info(methodName + "entry");
        KiteConnect kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setUserId(userId);
        kiteConnect.setAccessToken(accessToken);
        Map<String, List<Position>> positionsMap = kiteConnect.getPositions();
        log.debug(methodName + "position net size: {}", positionsMap.get("net").size());
        log.debug(methodName + "position day size: {}", positionsMap.get("day").size());
        log.info(methodName + "exit");
        return positionsMap;
    }


}
