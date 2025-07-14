package com.example.algotrading.model.request;

import lombok.Data;

@Data
public class BackTestRequest {

    private String equityId;
    private String fromDate;
    private String toDate;
    private String interval;

    private int emaShortPeriod; // e.g. 10
    private int emaLongPeriod;  // e.g. 20
    private int rsiPeriod;
    private int entryRsiThreshold;
    private double stopLossPercent;
    private double takeProfitPercent;
    private boolean useTrailingStopLoss;
}
