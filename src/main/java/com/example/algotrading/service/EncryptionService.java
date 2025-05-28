package com.example.algotrading.service;

import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EncryptionService {
    private final AES256TextEncryptor encryptor;

    public EncryptionService(@Value("${jasypt.encryptor.password}") String password) {
        encryptor = new AES256TextEncryptor();
        encryptor.setPassword(password);
    }

    public String encrypt(String plainText) {
        return encryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        return encryptor.decrypt(encryptedText);
    }
}
