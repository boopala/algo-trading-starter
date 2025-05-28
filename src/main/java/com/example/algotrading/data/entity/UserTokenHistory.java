package com.example.algotrading.data.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_token_history")
@Data
public class UserTokenHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String accessToken;
    private Instant createdAt;

}
