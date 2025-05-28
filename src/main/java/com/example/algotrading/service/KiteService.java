package com.example.algotrading.service;

import com.example.algotrading.model.response.*;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import com.zerodhatech.models.Profile;
import com.zerodhatech.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class KiteService {
    @Value("${kite.api-key}")
    private String apiKey;

    @Value("${kite.api-secret}")
    private String apiSecret;

    @Value("${kite.login-url}")
    String kiteLoginUrl;

    @Autowired
    UserTokenService userTokenService;

    public String generateLoginUrl() {
        return kiteLoginUrl + apiKey;
    }

    public TokenResponse generateAccessToken(String requestToken) throws Exception, KiteException {
        KiteConnect kiteConnect = new KiteConnect(apiKey);
        // Get access token using request token
        User user = kiteConnect.generateSession(requestToken, apiSecret);
        kiteConnect.setUserId(user.userId);

        kiteConnect.setAccessToken(user.accessToken);
        kiteConnect.setPublicToken(user.publicToken);

        // Optional: Store access token in DB or file here
        userTokenService.saveOrUpdateToken(user.userId, user.accessToken);

        // Return response DTO
        TokenResponse response = new TokenResponse();
        response.setAccessToken(user.accessToken);
        response.setPublicToken(user.publicToken);
        response.setUserId(user.userId);
        return response;
    }

    public Profile getUserProfile(String accessToken, String userId) throws IOException, KiteException {
        KiteConnect kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setUserId(userId);
        kiteConnect.setAccessToken(accessToken);
        return kiteConnect.getProfile();
    }

    public List<Holding> getHoldings(String accessToken, String userId) throws IOException, KiteException {
        KiteConnect kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setUserId(userId);
        kiteConnect.setAccessToken(accessToken);
        return kiteConnect.getHoldings();
    }
}
