package com.example.algotrading.data.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(name = "created_dt", updatable = false)
    private LocalDateTime createdAt;

}
