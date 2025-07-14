package com.example.algotrading.model;

public enum SellType {

    STRATEGY_SELL,   // Sell triggered by strategy logic (e.g. indicator crossover)
    STOP_LOSS_SELL,  // Sell triggered when price hits predefined stop loss
    TAKE_PROFIT_SELL, // Sell triggered when price hits predefined take profit
    FINAL_CLOSE

}
