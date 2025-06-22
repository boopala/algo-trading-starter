package com.example.algotrading.model.response;

import lombok.Data;

import java.util.List;

@Data
public class CustomIndicatorResponse {
    public List<Double> values; // for SMA, EMA, RSI
    public List<Double> macdValues;    // MACD Line
    public List<Double> signalValues;  // MACD Signal Line
    public List<Double> histogramValues; // MACD Histogram

    // Constructor for indicators like SMA, EMA, RSI
    public CustomIndicatorResponse(List<Double> values) {
        this.values = values;
    }

    // Constructor for MACD
    public CustomIndicatorResponse(List<Double> macdValues, List<Double> signalValues, List<Double> histogramValues) {
        this.macdValues = macdValues;
        this.signalValues = signalValues;
        this.histogramValues = histogramValues;
    }
}
