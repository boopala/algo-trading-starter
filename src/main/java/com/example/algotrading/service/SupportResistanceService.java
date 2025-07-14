package com.example.algotrading.service;

import com.example.algotrading.model.SwingPoint;
import com.example.algotrading.model.response.HistoricalData;
import com.example.algotrading.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SupportResistanceService {

    public void markSwingHighLow(List<HistoricalData> data, int range) {
        String methodName = "markSwingHighLow ";
        log.info(methodName + "entry");
        for (int i = range; i < data.size() - range; i++) {
            boolean isHigh = true, isLow = true;

            for (int j = 1; j <= range; j++) {
                if (data.get(i).high <= data.get(i - j).high ||
                        data.get(i).high <= data.get(i + j).high) {
                    isHigh = false;
                }
                if (data.get(i).low >= data.get(i - j).low ||
                        data.get(i).low >= data.get(i + j).low) {
                    isLow = false;
                }
            }

            data.get(i).setResistance(isHigh);
            data.get(i).setSupport(isLow);
        }
        log.info(methodName + "exit");
    }

    public void detectSupportResistance(List<HistoricalData> data, int lookBack, int lookAhead, double thresholdPercent) {
        if (data == null || data.size() < lookBack + lookAhead + 1) return;

        List<SwingPoint> rawPoints = new ArrayList<>();

        for (int i = lookBack; i < data.size() - lookAhead; i++) {
            HistoricalData current = data.get(i);
            boolean isHigh = true;
            boolean isLow = true;

            double currentHigh = current.high;
            double currentLow = current.low;

            for (int j = i - lookBack; j <= i + lookAhead; j++) {
                if (j == i) continue;
                if (data.get(j).high >= currentHigh * (1 - thresholdPercent)) {
                    isHigh = false;
                }
                if (data.get(j).low <= currentLow * (1 + thresholdPercent)) {
                    isLow = false;
                }
            }

            if (isHigh) {
                rawPoints.add(new SwingPoint(DateUtils.parseISOOffsetDate(current.timeStamp), currentHigh, false)); // Resistance
            } else if (isLow) {
                rawPoints.add(new SwingPoint(DateUtils.parseISOOffsetDate(current.timeStamp), currentLow, true)); // Support
            }
        }

        // Filter nearby points
        List<SwingPoint> cleanPoints = filterNearbySwingPoints(rawPoints, 5, 0.002); // 5 candle gap, 0.2% price diff

        // Map to original HistoricalData list
        for (HistoricalData d : data) {
            for (SwingPoint sp : cleanPoints) {
                if (DateUtils.parseISOOffsetDate(d.timeStamp).equals(sp.time)) {
                    d.setSupport(sp.isSupport);
                    d.setResistance(!sp.isSupport);
                    break;
                }
            }
        }
    }

    private List<SwingPoint> filterNearbySwingPoints(List<SwingPoint> points, int minCandleGap, double minPricePercent) {
        List<SwingPoint> filtered = new ArrayList<>();

        for (SwingPoint candidate : points) {
            boolean tooClose = false;

            for (SwingPoint existing : filtered) {
                long timeDiff = Math.abs(candidate.time.getTime() - existing.time.getTime());
                long approxCandleGap = timeDiff / (1000 * 60); // assumes 1 candle = 1 minute

                double priceDiff = Math.abs(candidate.price - existing.price);
                double priceThreshold = existing.price * minPricePercent;

                if (approxCandleGap < minCandleGap && priceDiff < priceThreshold) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                filtered.add(candidate);
            }
        }

        return filtered;
    }


}
