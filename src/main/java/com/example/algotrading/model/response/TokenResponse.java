package com.example.algotrading.model.response;

import lombok.Data;

@Data
public class TokenResponse {
    private String accessToken;
    private String publicToken;
    private String userId;
}
