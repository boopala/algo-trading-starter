package com.example.algotrading.data.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "equity")
@Data
public class Equity extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long equityId;
    private String instrumentToken;
    private String exchangeToken;
    private String tradingSymbol;
    @ManyToOne
    @JoinColumn(name = "equity_name_id")
    private EquityName equityName;
    @ManyToOne
    @JoinColumn(name = "instrument_type_id")
    private InstrumentType instrumentType;
    @ManyToOne
    @JoinColumn(name = "segment_id")
    private Segment segment;
    @ManyToOne
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;
    private boolean isDeleted;
}
