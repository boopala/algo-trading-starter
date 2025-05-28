package com.example.algotrading.strategy;

import com.example.algotrading.model.MarketData;
import com.example.algotrading.model.OrderType;
import com.zerodhatech.models.Position;

public class StrategyEngine {

    private TradingStrategy strategy;

    public StrategyEngine(TradingStrategy strategy) {
        this.strategy = strategy;
    }

    public void evaluate(MarketData data, Position currentPosition) {
        if (currentPosition == null && strategy.shouldEnterTrade(data)) {
            placeOrder(data.getSymbol(), OrderType.BUY);
        } else if (currentPosition != null && strategy.shouldExitTrade(currentPosition, data)) {
            placeOrder(data.getSymbol(), OrderType.SELL);
        }
    }

    private void placeOrder(String symbol, OrderType type) {
        // Integrate with Kite Connect API to place order
        System.out.println("Placing " + type + " order for " + symbol);
    }

}
