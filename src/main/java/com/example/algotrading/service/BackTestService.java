package com.example.algotrading.service;

import com.example.algotrading.model.BackTestResult;
import com.example.algotrading.model.SellType;
import com.example.algotrading.model.TradeEntryExit;
import com.example.algotrading.model.response.HistoricalData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BackTestService {

    @Autowired
    private IndicatorService indicatorService;

    /*public BackTestResult backTestEmaRsiStrategy(List<HistoricalData> data,
                                                 int emaShortPeriod,
                                                 int emaLongPeriod,
                                                 int rsiPeriod,
                                                 int entryRSIThreshold,
                                                 double stopLossPercent,
                                                 double takeProfitPercent, String interval) {
        String methodName = "backTestEmaRsiStrategy ";
        log.info(methodName + "entry");
        if (data == null || data.size() < 30) {
            throw new IllegalArgumentException("Not enough data for back testing.");
        }

        // Prepare BarSeries
        List<Double> open = data.stream().map(d -> d.open).collect(Collectors.toList());
        List<Double> high = data.stream().map(d -> d.high).collect(Collectors.toList());
        List<Double> low = data.stream().map(d -> d.low).collect(Collectors.toList());
        List<Double> close = data.stream().map(d -> d.close).collect(Collectors.toList());
        List<Double> volume = data.stream().map(d -> Long.valueOf(d.volume).doubleValue()).collect(Collectors.toList());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"); // or your custom format
        List<ZonedDateTime> times = data.stream()
                .map(d -> ZonedDateTime.parse(d.timeStamp, formatter))
                .collect(Collectors.toList());

        BarSeries series = indicatorService.buildBarSeries(times, open, high, low, close, volume, interval);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        EMAIndicator emaShort = new EMAIndicator(closePrice, emaShortPeriod);
        EMAIndicator emaLong = new EMAIndicator(closePrice, emaLongPeriod);
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiPeriod);

        List<TradeEntryExit> trades = new ArrayList<>();
        boolean inPosition = false;
        double entryPrice = 0;
        double trailEntryPrice = 0;
        ZonedDateTime entryTime = null;
        boolean trailingStopLoss = false;

        for (int i = 1; i < series.getBarCount(); i++) {
            double prevEmaShort = emaShort.getValue(i - 1).doubleValue();
            double prevEmaLong = emaLong.getValue(i - 1).doubleValue();
            double currEmaShort = emaShort.getValue(i).doubleValue();
            double currEmaLong = emaLong.getValue(i).doubleValue();
            double prevRsi = rsi.getValue(i - 1).doubleValue();
            double currRsi = rsi.getValue(i).doubleValue();

            double currentPrice = closePrice.getValue(i).doubleValue();
            double currentLowPrice = lowPrice.getValue(i).doubleValue();
            double currentHighPrice = highPrice.getValue(i).doubleValue();

            // Buy condition
            boolean buySignal = !inPosition &&
                    prevEmaShort <= prevEmaLong &&
                    currEmaShort > currEmaLong &&
                    prevRsi < entryRSIThreshold &&
                    currRsi >= entryRSIThreshold;

            // Strategy-based Sell condition
            *//*boolean strategySell = inPosition &&
                    ((prevEmaShort >= prevEmaLong && currEmaShort < currEmaLong) ||
                            currRsi > 80);*//*

            // Stop-Loss or Take-Profit Sell
            boolean stopLossSell = isStopLossSell(inPosition, currentPrice, currentLowPrice, entryPrice, trailEntryPrice, stopLossPercent);
            boolean takeProfitSell = isTakeProfitSell(inPosition, currentPrice, currentHighPrice, entryPrice, takeProfitPercent, trailingStopLoss);
            //boolean stopLossSell = inPosition && (currentPrice <= (trailEntryPrice == 0 ? entryPrice * (1 - stopLossPercent / 100) : trailEntryPrice * (1 - stopLossPercent / 100)));
            //boolean takeProfitSell = inPosition && (currentPrice >= entryPrice * (1 + takeProfitPercent / 100)) && !trailingStopLoss;

            if (takeProfitSell) {
                trailingStopLoss = true;
                trailEntryPrice = currentPrice;
            }

            if (buySignal && i >= 14) {
                inPosition = true;
                entryPrice = currentPrice;
                entryTime = series.getBar(i).getEndTime();
            } else if (stopLossSell) {
                ZonedDateTime exitTime = series.getBar(i).getEndTime();
                double sellPrice = trailingStopLoss ? trailEntryPrice * (1 - stopLossPercent / 100) : entryPrice * (1 - stopLossPercent / 100);
                double profit = sellPrice - entryPrice;
                double profitPct = (profit / entryPrice) * 100;

                trades.add(new TradeEntryExit(
                        Date.from(entryTime.toInstant()),
                        entryPrice,
                        Date.from(exitTime.toInstant()),
                        sellPrice,
                        profit,
                        profitPct,
                        trailingStopLoss ? SellType.TAKE_PROFIT_SELL : SellType.STOP_LOSS_SELL
                ));
                inPosition = false;
                trailEntryPrice = 0;
                trailingStopLoss = false;
                log.info(methodName + "takeProfitSell: {}, trailingStopLoss: {}, trailEntryPrice: {}, currentPrice: {}, entryTime: {}, exitTime: {}, sellPrice: {}, profit: {}",
                        takeProfitSell, trailingStopLoss, trailEntryPrice, currentPrice, entryTime, exitTime, sellPrice, profit);
            }
            log.info(methodName + "buySignal: {}, stopLossSell: {}, takeProfitSell: {}, trailingStopLoss: {}, trailEntryPrice: {}, currentPrice: {}, entryTime: {}",
                    buySignal, stopLossSell, takeProfitSell, trailingStopLoss, trailEntryPrice, currentPrice, entryTime);
        }

        // Exit open position at end
        if (inPosition) {
            double exitPrice = closePrice.getValue(series.getBarCount() - 1).doubleValue();
            ZonedDateTime exitTime = series.getBar(series.getBarCount() - 1).getEndTime();
            double profit = exitPrice - entryPrice;
            double profitPct = (profit / entryPrice) * 100;

            trades.add(new TradeEntryExit(
                    Date.from(entryTime.toInstant()),
                    entryPrice,
                    Date.from(exitTime.toInstant()),
                    exitPrice,
                    profit,
                    profitPct,
                    SellType.FINAL_CLOSE
            ));
        }

        double totalProfit = trades.stream().filter(t -> t.getProfit() >= 0).mapToDouble(TradeEntryExit::getProfit).sum();
        double totalLoss = trades.stream().filter(t -> t.getProfit() < 0).mapToDouble(TradeEntryExit::getProfit).sum();
        log.info(methodName + "totalProfit: {}, totalLoss: {}", totalProfit, totalLoss);
        log.info(methodName + "exit");
        return new BackTestResult(trades, data, totalProfit, totalLoss);
    }

    private boolean isTakeProfitSell(boolean inPosition, double currentPrice, double highPrice, double entryPrice, double takeProfitPercent, boolean trailingStopLoss) {
        double checkPrice = Math.max(currentPrice, highPrice);
        return inPosition &&
                (
                        checkPrice >= entryPrice * (1 + takeProfitPercent / 100)
                )
                && !trailingStopLoss;
    }

    private boolean isStopLossSell(boolean inPosition, double currentPrice, double lowPrice, double entryPrice, double trailEntryPrice, double stopLossPercent) {
        double checkPrice = Math.min(lowPrice, currentPrice);
        return inPosition &&
                (
                        checkPrice <=
                                (
                                        trailEntryPrice == 0 ?
                                                entryPrice * (1 - stopLossPercent / 100) :
                                                trailEntryPrice * (1 - stopLossPercent / 100)
                                )
                );
    }*/

    /*public BackTestResult backTestEmaRsiStrategy(List<HistoricalData> data,
                                                 int emaShortPeriod,
                                                 int emaLongPeriod,
                                                 int rsiPeriod,
                                                 int entryRSIThreshold,
                                                 double stopLossPercent,
                                                 double takeProfitPercent,
                                                 String interval) {
        String methodName = "backTestEmaRsiStrategy ";
        log.info(methodName + "entry");

        if (data == null || data.size() < 30) {
            throw new IllegalArgumentException("Not enough data for backTesting.");
        }

        // 1. Extract price/volume/time
        List<Double> open = data.stream().map(d -> d.open).collect(Collectors.toList());
        List<Double> high = data.stream().map(d -> d.high).collect(Collectors.toList());
        List<Double> low = data.stream().map(d -> d.low).collect(Collectors.toList());
        List<Double> close = data.stream().map(d -> d.close).collect(Collectors.toList());
        List<Double> volume = data.stream().map(d -> (double) d.volume).collect(Collectors.toList());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        List<ZonedDateTime> times = data.stream()
                .map(d -> ZonedDateTime.parse(d.timeStamp, formatter))
                .collect(Collectors.toList());

        BarSeries series = indicatorService.buildBarSeries(times, open, high, low, close, volume, interval);

        // 2. Indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);
        EMAIndicator emaShort = new EMAIndicator(closePrice, emaShortPeriod);
        EMAIndicator emaLong = new EMAIndicator(closePrice, emaLongPeriod);
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiPeriod);

        // 3. Trade logic
        List<TradeEntryExit> trades = new ArrayList<>();
        boolean inPosition = false;
        boolean trailingStopLoss = false;
        double entryPrice = 0;
        double trailEntryPrice = 0;
        ZonedDateTime entryTime = null;

        for (int i = 1; i < series.getBarCount(); i++) {
            double prevEmaShort = emaShort.getValue(i - 1).doubleValue();
            double prevEmaLong = emaLong.getValue(i - 1).doubleValue();
            double currEmaShort = emaShort.getValue(i).doubleValue();
            double currEmaLong = emaLong.getValue(i).doubleValue();
            double prevRsi = rsi.getValue(i - 1).doubleValue();
            double currRsi = rsi.getValue(i).doubleValue();

            double currentPrice = closePrice.getValue(i).doubleValue();
            double currentLow = lowPrice.getValue(i).doubleValue();
            double currentHigh = highPrice.getValue(i).doubleValue();

            // ✅ Buy Signal
            boolean buySignal = !inPosition &&
                    prevEmaShort <= prevEmaLong &&
                    currEmaShort > currEmaLong &&
                    prevRsi < entryRSIThreshold &&
                    currRsi >= entryRSIThreshold;

            // ✅ Take-Profit Check (initial or update trail)
            boolean takeProfitHit = inPosition && !trailingStopLoss &&
                    currentHigh >= entryPrice * (1 + takeProfitPercent / 100);

            // ✅ Start or update trailing stop
            if (takeProfitHit) {
                trailingStopLoss = true;
                trailEntryPrice = currentPrice;
            } else if (trailingStopLoss && currentPrice > trailEntryPrice) {
                trailEntryPrice = currentPrice;
            }

            // ✅ Stop-Loss Check (uses trail price if available)
            double stopPrice = trailingStopLoss ? trailEntryPrice : entryPrice;
            boolean stopLossHit = inPosition &&
                    currentLow <= stopPrice * (1 - stopLossPercent / 100);

            // === Apply Buy ===
            if (buySignal && i >= rsiPeriod) {
                inPosition = true;
                entryPrice = currentPrice;
                entryTime = series.getBar(i).getEndTime();
            }

            // === Apply Sell ===
            else if (stopLossHit) {
                double sellPrice = stopPrice * (1 - stopLossPercent / 100);
                ZonedDateTime exitTime = series.getBar(i).getEndTime();
                double profit = sellPrice - entryPrice;
                double profitPct = (profit / entryPrice) * 100;

                trades.add(new TradeEntryExit(
                        Date.from(entryTime.toInstant()),
                        entryPrice,
                        Date.from(exitTime.toInstant()),
                        sellPrice,
                        profit,
                        profitPct,
                        trailingStopLoss ? SellType.TAKE_PROFIT_SELL : SellType.STOP_LOSS_SELL
                ));

                inPosition = false;
                trailingStopLoss = false;
                trailEntryPrice = 0;
            }
        }

        // 4. Final Close if Still Holding
        if (inPosition) {
            double exitPrice = closePrice.getValue(series.getBarCount() - 1).doubleValue();
            ZonedDateTime exitTime = series.getBar(series.getBarCount() - 1).getEndTime();
            double profit = exitPrice - entryPrice;
            double profitPct = (profit / entryPrice) * 100;

            trades.add(new TradeEntryExit(
                    Date.from(entryTime.toInstant()),
                    entryPrice,
                    Date.from(exitTime.toInstant()),
                    exitPrice,
                    profit,
                    profitPct,
                    SellType.FINAL_CLOSE
            ));
        }

        // 5. Summary
        double totalProfit = trades.stream().filter(t -> t.getProfit() >= 0).mapToDouble(TradeEntryExit::getProfit).sum();
        double totalLoss = trades.stream().filter(t -> t.getProfit() < 0).mapToDouble(TradeEntryExit::getProfit).sum();

        log.info(methodName + "totalProfit: {}, totalLoss: {}", totalProfit, totalLoss);
        return new BackTestResult(trades, data, totalProfit, totalLoss);
    }*/

    public BackTestResult backTestEmaRsiStrategy(List<HistoricalData> data,
                                                 int emaShortPeriod,
                                                 int emaLongPeriod,
                                                 int rsiPeriod,
                                                 int entryRSIThreshold,
                                                 double stopLossPercent,
                                                 double takeProfitPercent,
                                                 String interval,
                                                 boolean useTrailingStopLoss) {
        String methodName = "backTestEmaRsiStrategy ";
        log.info(methodName + "entry");

        if (data == null || data.size() < 30) {
            throw new IllegalArgumentException("Not enough data for backTesting.");
        }

        List<Double> open = new ArrayList<>(data.size());
        List<Double> high = new ArrayList<>(data.size());
        List<Double> low = new ArrayList<>(data.size());
        List<Double> close = new ArrayList<>(data.size());
        List<Double> volume = new ArrayList<>(data.size());
        List<ZonedDateTime> times = new ArrayList<>(data.size());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

        for (HistoricalData d : data) {
            open.add(d.open);
            high.add(d.high);
            low.add(d.low);
            close.add(d.close);
            volume.add((double) d.volume);
            times.add(ZonedDateTime.parse(d.timeStamp, formatter));
        }

        BarSeries series = indicatorService.buildBarSeries(times, open, high, low, close, volume, interval);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        HighPriceIndicator highPrice = new HighPriceIndicator(series);
        LowPriceIndicator lowPrice = new LowPriceIndicator(series);
        EMAIndicator emaShort = new EMAIndicator(closePrice, emaShortPeriod);
        EMAIndicator emaLong = new EMAIndicator(closePrice, emaLongPeriod);
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiPeriod);

        List<TradeEntryExit> trades = new ArrayList<>();
        boolean inPosition = false;
        boolean trailingStopLossActive = false;
        double entryPrice = 0;
        double trailEntryPrice = 0;
        ZonedDateTime entryTime = null;

        for (int i = 1; i < series.getBarCount(); i++) {
            double prevEmaShort = emaShort.getValue(i - 1).doubleValue();
            double prevEmaLong = emaLong.getValue(i - 1).doubleValue();
            double currEmaShort = emaShort.getValue(i).doubleValue();
            double currEmaLong = emaLong.getValue(i).doubleValue();
            double prevRsi = rsi.getValue(i - 1).doubleValue();
            double currRsi = rsi.getValue(i).doubleValue();

            double currClose = closePrice.getValue(i).doubleValue();
            double currHigh = highPrice.getValue(i).doubleValue();
            double currLow = lowPrice.getValue(i).doubleValue();

            // === Buy Signal ===
            boolean buySignal = !inPosition &&
                    prevEmaShort <= prevEmaLong &&
                    currEmaShort > currEmaLong &&
                    prevRsi < entryRSIThreshold &&
                    currRsi >= entryRSIThreshold;

            if (buySignal && i >= rsiPeriod) {
                inPosition = true;
                entryPrice = currClose;
                entryTime = series.getBar(i).getEndTime();
                continue;
            }

            if (!inPosition) continue;

            boolean takeProfitHit = false;
            if (useTrailingStopLoss) {
                if (!trailingStopLossActive &&
                        currHigh >= entryPrice * (1 + takeProfitPercent / 100)) {
                    trailingStopLossActive = true;
                    trailEntryPrice = currClose;
                } else if (trailingStopLossActive && currClose > trailEntryPrice) {
                    trailEntryPrice = currClose;
                }
            } else {
                // classic take-profit hit
                takeProfitHit = currHigh >= entryPrice * (1 + takeProfitPercent / 100);
            }

            double stopPrice = useTrailingStopLoss && trailingStopLossActive ? trailEntryPrice : entryPrice;
            boolean stopLossHit = currLow <= stopPrice * (1 - stopLossPercent / 100);

            if (stopLossHit || (!useTrailingStopLoss && takeProfitHit)) {
                double sellPrice = stopLossHit
                        ? stopPrice * (1 - stopLossPercent / 100)
                        : entryPrice * (1 + takeProfitPercent / 100);

                ZonedDateTime exitTime = series.getBar(i).getEndTime();
                double profit = sellPrice - entryPrice;
                double profitPct = (profit / entryPrice) * 100;

                trades.add(new TradeEntryExit(
                        Date.from(entryTime.toInstant()),
                        entryPrice,
                        Date.from(exitTime.toInstant()),
                        sellPrice,
                        profit,
                        profitPct,
                        stopLossHit
                                ? (trailingStopLossActive ? SellType.TAKE_PROFIT_SELL : SellType.STOP_LOSS_SELL)
                                : SellType.TAKE_PROFIT_SELL
                ));

                inPosition = false;
                trailingStopLossActive = false;
                trailEntryPrice = 0;
            }
        }

        if (inPosition) {
            double exitPrice = closePrice.getValue(series.getBarCount() - 1).doubleValue();
            ZonedDateTime exitTime = series.getBar(series.getBarCount() - 1).getEndTime();
            double profit = exitPrice - entryPrice;
            double profitPct = (profit / entryPrice) * 100;

            trades.add(new TradeEntryExit(
                    Date.from(entryTime.toInstant()),
                    entryPrice,
                    Date.from(exitTime.toInstant()),
                    exitPrice,
                    profit,
                    profitPct,
                    SellType.FINAL_CLOSE
            ));
        }

        double totalProfit = trades.stream().filter(t -> t.getProfit() >= 0).mapToDouble(TradeEntryExit::getProfit).sum();
        double totalLoss = trades.stream().filter(t -> t.getProfit() < 0).mapToDouble(TradeEntryExit::getProfit).sum();

        log.info(methodName + "totalProfit: {}, totalLoss: {}, trailingEnabled: {}", totalProfit, totalLoss, useTrailingStopLoss);
        return new BackTestResult(trades, data, totalProfit, totalLoss);
    }

}
