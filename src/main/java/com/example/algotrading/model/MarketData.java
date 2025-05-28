package com.example.algotrading.model;

import lombok.Data;

import java.util.List;

@Data
public class MarketData {
    private String symbol;
    private double lastTradedPrice;
    private double previousClose;
    private double high;
    private double low;
    private double volume;
    private List<Double> recentPrices;
}
