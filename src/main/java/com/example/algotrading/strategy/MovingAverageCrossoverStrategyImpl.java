package com.example.algotrading.strategy;

import com.example.algotrading.model.MarketData;
import com.zerodhatech.models.Position;

import java.util.List;

public class MovingAverageCrossoverStrategyImpl implements TradingStrategy {

    /**
     * @param data
     * @return
     */
    @Override
    public boolean shouldEnterTrade(MarketData data) {
        List<Double> prices = data.getRecentPrices();
        double shortMA = calculateMovingAverage(prices, 5);
        double longMA = calculateMovingAverage(prices, 20);
        return shortMA > longMA;
    }

    /**
     * @param position
     * @param data
     * @return
     */
    @Override
    public boolean shouldExitTrade(Position position, MarketData data) {
        // Exit condition: short MA crosses below long MA
        List<Double> prices = data.getRecentPrices();
        double shortMA = calculateMovingAverage(prices, 5);
        double longMA = calculateMovingAverage(prices, 20);
        return shortMA < longMA;
    }

    /**
     * @return
     */
    @Override
    public String getStrategyName() {
        return "Moving Average Crossover";
    }

    private double calculateMovingAverage(List<Double> prices, int period) {
        if (prices.size() < period) return 0;
        return prices.subList(prices.size() - period, prices.size())
                .stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
