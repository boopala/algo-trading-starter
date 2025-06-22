package com.example.algotrading.model.response;

import lombok.Data;

@Data
public class IndicatorResponse {

    public double sma;
    public double ema;
    public double rsi;
    public double macd;
    public double macdSignal;
    public double bollingerUpper;
    public double bollingerMiddle;
    public double bollingerLower;
    public double[] fibonacciLevels;

    public IndicatorResponse(double sma, double ema, double rsi,
                             double macd, double macdSignal,
                             double bollingerUpper, double bollingerMiddle, double bollingerLower,
                             double[] fibonacciLevels) {
        this.sma = sma;
        this.ema = ema;
        this.rsi = rsi;
        this.macd = macd;
        this.macdSignal = macdSignal;
        this.bollingerUpper = bollingerUpper;
        this.bollingerMiddle = bollingerMiddle;
        this.bollingerLower = bollingerLower;
        this.fibonacciLevels = fibonacciLevels;
    }
}
