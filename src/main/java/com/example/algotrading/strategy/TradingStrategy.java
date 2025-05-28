package com.example.algotrading.strategy;

import com.example.algotrading.model.MarketData;
import com.zerodhatech.models.Position;

public interface TradingStrategy {
    boolean shouldEnterTrade(MarketData data);
    boolean shouldExitTrade(Position position, MarketData data);
    String getStrategyName();
}
