package com.example.algotrading.model;

import com.example.algotrading.model.response.HistoricalData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BackTestResult {
    private List<TradeEntryExit> trades;
    private List<HistoricalData> historicalData;
    private double totalProfit;
    private double totalLoss;
}
