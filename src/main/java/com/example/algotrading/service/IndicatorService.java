package com.example.algotrading.service;

import com.example.algotrading.model.request.CustomIndicatorRequest;
import com.example.algotrading.model.response.CustomIndicatorResponse;
import com.example.algotrading.model.response.IndicatorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class IndicatorService {

    public IndicatorResponse calculateIndicators(List<Double> open, List<Double> high,
                                                 List<Double> low, List<Double> close,
                                                 List<Double> volume) {
        String methodName = "calculateIndicators ";
        log.info(methodName + "entry");
        BarSeries series = new BaseBarSeriesBuilder().withName("equity_series").build();

        for (int i = 0; i < close.size(); i++) {
            series.addBar(
                    ZonedDateTime.now().minusDays(close.size() - i),
                    open.get(i),
                    high.get(i),
                    low.get(i),
                    close.get(i),
                    volume.get(i)
            );
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // SMA (14)
        SMAIndicator sma = new SMAIndicator(closePrice, 14);

        // EMA (14)
        EMAIndicator ema = new EMAIndicator(closePrice, 14);

        // RSI (14)
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);

        // MACD (12, 26, 9)
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);

        // Bollinger Bands (20, 2.0)
        SMAIndicator smaBB = new SMAIndicator(closePrice, 20);
        StandardDeviationIndicator sd = new StandardDeviationIndicator(closePrice, 20);
        BollingerBandsMiddleIndicator bbm = new BollingerBandsMiddleIndicator(smaBB);
        BollingerBandsUpperIndicator bbu = new BollingerBandsUpperIndicator(bbm, sd);
        BollingerBandsLowerIndicator bbl = new BollingerBandsLowerIndicator(bbm, sd);

        // Fibonacci (last high/low)
        double lastHigh = new HighPriceIndicator(series).getValue(series.getEndIndex()).doubleValue();
        double lastLow = new LowPriceIndicator(series).getValue(series.getEndIndex()).doubleValue();
        double[] fib = calculateFibonacci(lastHigh, lastLow);

        int endIndex = series.getEndIndex();
        log.info(methodName + "exit");
        return new IndicatorResponse(
                sma.getValue(endIndex).doubleValue(),
                ema.getValue(endIndex).doubleValue(),
                rsi.getValue(endIndex).doubleValue(),
                macd.getValue(endIndex).doubleValue(),
                macdSignal.getValue(endIndex).doubleValue(),
                bbu.getValue(endIndex).doubleValue(),
                bbm.getValue(endIndex).doubleValue(),
                bbl.getValue(endIndex).doubleValue(),
                fib
        );
    }

    private double[] calculateFibonacci(double high, double low) {
        String methodName = "calculateFibonacci ";
        log.info(methodName + "entry");
        double diff = high - low;
        log.info(methodName + "exit");
        return new double[]{
                high,
                high - 0.236 * diff,
                high - 0.382 * diff,
                high - 0.5 * diff,
                high - 0.618 * diff,
                low
        };
    }

    public CustomIndicatorResponse calculateCustomIndicator(CustomIndicatorRequest req) {
        BarSeries series = buildBarSeries(req.open, req.high, req.low, req.close, req.volume);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        List<Double> values = new ArrayList<>();

        switch (req.indicator) {
            case "SMA":
                SMAIndicator sma = new SMAIndicator(closePrice, req.period);
                for (int i = 0; i < series.getBarCount(); i++) {
                    values.add(sma.getValue(i).doubleValue());
                }
                return new CustomIndicatorResponse(values);

            case "EMA":
                EMAIndicator ema = new EMAIndicator(closePrice, req.period);
                for (int i = 0; i < series.getBarCount(); i++) {
                    values.add(ema.getValue(i).doubleValue());
                }
                return new CustomIndicatorResponse(values);

            case "RSI":
                RSIIndicator rsi = new RSIIndicator(closePrice, req.period);
                for (int i = 0; i < series.getBarCount(); i++) {
                    values.add(rsi.getValue(i).doubleValue());
                }
                return new CustomIndicatorResponse(values);

            case "MACD":
                MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
                EMAIndicator macdSignal = new EMAIndicator(macd, 9);

                List<Double> macdList = new ArrayList<>();
                List<Double> signalList = new ArrayList<>();
                List<Double> histogramList = new ArrayList<>();

                for (int i = 0; i < series.getBarCount(); i++) {
                    double macdValue = macd.getValue(i).doubleValue();
                    double signalValue = macdSignal.getValue(i).doubleValue();
                    double histogram = macdValue - signalValue;

                    macdList.add(macdValue);
                    signalList.add(signalValue);
                    histogramList.add(histogram);
                }
                return new CustomIndicatorResponse(macdList, signalList, histogramList);

            default:
                throw new IllegalArgumentException("Unknown indicator: " + req.indicator);
        }
    }

    private BarSeries buildBarSeries(List<Double> open, List<Double> high, List<Double> low,
                                     List<Double> close, List<Double> volume) {
        String methodName = "buildBarSeries ";
        log.info(methodName + "entry");
        BarSeries series = new BaseBarSeriesBuilder().withName("custom_series").build();
        for (int i = 0; i < close.size(); i++) {
            series.addBar(
                    ZonedDateTime.now().minusDays(close.size() - i),
                    open.get(i),
                    high.get(i),
                    low.get(i),
                    close.get(i),
                    volume.get(i)
            );
        }
        log.info(methodName + "exit");
        return series;
    }
}
