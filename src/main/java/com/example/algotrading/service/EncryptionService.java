package com.example.algotrading.service;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EncryptionService {
    private final AES256TextEncryptor encryptor;

    public EncryptionService(@Value("${jasypt.encryptor.password}") String password) {
        encryptor = new AES256TextEncryptor();
        encryptor.setPassword(password);
    }

    public String encrypt(String plainText) {
        String methodName = "encrypt ";
        log.info(methodName + "entry");
        return encryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        String methodName = "decrypt ";
        log.info(methodName + "entry");
        return encryptor.decrypt(encryptedText);
    }
}
