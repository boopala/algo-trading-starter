package com.example.algotrading.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holding {

    private Long id;
    private String product;
    private Double lastPrice;
    private String price;
    private String tradingSymbol;
    private Integer t1Quantity;
    private String collateralQuantity;
    private String collateraltype;
    private String isin;
    private Double pnl;
    private Integer quantity;
    private String realisedQuantity;
    private Double averagePrice;
    private String exchange;
    private String instrumentToken;
    private Integer usedQuantity;
    private Integer authorisedQuantity;
    private LocalDateTime authorisedDate;
    private Boolean discrepancy;
    private Double dayChange;
    private Double dayChangePercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
}

