package com.example.algotrading.model;

import java.util.Date;

public class SwingPoint {
    public Date time;
    public double price;
    public boolean isSupport; // true = support, false = resistance

    public SwingPoint(Date time, double price, boolean isSupport) {
        this.time = time;
        this.price = price;
        this.isSupport = isSupport;
    }
}
