package com.example.algotrading.model;

public class SRDetectionParams {
    public int lookBack;
    public int lookAhead;
    public double thresholdPercent;

    public SRDetectionParams(int lookBack, int lookAhead, double thresholdPercent) {
        this.lookBack = lookBack;
        this.lookAhead = lookAhead;
        this.thresholdPercent = thresholdPercent;
    }
}