package com.example.algotrading.service;

import com.example.algotrading.data.entity.UserToken;
import com.example.algotrading.data.entity.UserTokenHistory;
import com.example.algotrading.data.repository.UserTokenHistoryRepository;
import com.example.algotrading.data.repository.UserTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class UserTokenService {

    private final UserTokenRepository userTokenRepository;
    private final UserTokenHistoryRepository userTokenHistoryRepository;
    private final EncryptionService encryptionService;

    @Autowired
    public UserTokenService(UserTokenRepository userTokenRepository, UserTokenHistoryRepository userTokenHistoryRepository, EncryptionService encryptionService) {
        this.userTokenRepository = userTokenRepository;
        this.userTokenHistoryRepository = userTokenHistoryRepository;
        this.encryptionService = encryptionService;
    }

    public void saveOrUpdateToken(String userId, String accessToken) {
        String methodName = "saveOrUpdateToken ";
        log.info(methodName + "entry");
        UserToken token = new UserToken();
        token.setUserId(userId);
        token.setCreatedAt(Instant.now());
        String encryptedToken = encryptionService.encrypt(accessToken);
        token.setAccessToken(encryptedToken);
        userTokenRepository.save(token);

        // Save to history
        UserTokenHistory history = new UserTokenHistory();
        history.setUserId(userId);
        history.setAccessToken(encryptedToken);
        history.setCreatedAt(Instant.now());
        userTokenHistoryRepository.save(history);
        log.info(methodName + "exit");
    }

    public Optional<String> getAccessTokenByUserId(String userId) {
        String methodName = "getAccessTokenByUserId ";
        log.info(methodName + "entry");
        Optional<String> accessToken = userTokenRepository.findById(userId).map(UserToken::getAccessToken);
        log.info(methodName + "exit");
        return accessToken;
    }
}
