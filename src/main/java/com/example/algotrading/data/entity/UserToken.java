package com.example.algotrading.data.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_token")
@Data
public class UserToken {

    @Id
    private String userId;

    @Column(nullable = false)
    private String accessToken;

    @Column
    private Instant createdAt;
}
