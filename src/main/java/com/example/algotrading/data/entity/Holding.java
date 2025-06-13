package com.example.algotrading.data.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "holding")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;
    @Column(name = "last_price")
    private Double lastPrice;
    private String price;
    @Column(name = "trading_symbol")
    private String tradingSymbol;
    @Column(name = "t1quantity")
    private Integer t1Quantity;
    @Column(name = "collateral_quantity")
    private String collateralQuantity;
    private String collateraltype;
    private String isin;
    private Double pnl;
    private Integer quantity;
    @Column(name = "realised_quantity")
    private String realisedQuantity;
    @Column(name = "average_price")
    private Double averagePrice;
    private String exchange;
    @Column(name = "instrument_token")
    private String instrumentToken;
    @Column(name = "used_quantity")
    private Integer usedQuantity;
    @Column(name = "authorised_quantity")
    private Integer authorisedQuantity;

    @Column(name = "authorised_date", columnDefinition = "TIMESTAMP")
    private LocalDateTime authorisedDate;

    private Boolean discrepancy;
    @Column(name = "day_change")
    private Double dayChange;
    @Column(name = "day_change_percentage")
    private Double dayChangePercentage;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isDeleted;

    /*@PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }*/
}
