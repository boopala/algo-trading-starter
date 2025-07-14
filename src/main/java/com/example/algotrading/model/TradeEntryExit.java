package com.example.algotrading.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class TradeEntryExit {
    private Date buyTime;
    private double buyPrice;
    private Date sellTime;
    private double sellPrice;
    private double profit;
    private double profitPercent;
    private SellType sellType;
}
