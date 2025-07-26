package com.example.algotrading.service;

import com.example.algotrading.model.SRDetectionParams;
import com.example.algotrading.model.request.CustomIndicatorRequest;
import com.example.algotrading.model.response.CustomIndicatorResponse;
import com.example.algotrading.model.response.IndicatorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
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
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IndicatorService {

    private static final Map<String, Duration> INTERVAL_DURATION_MAP = Map.ofEntries(
            Map.entry("day", Duration.ofDays(1)),
            Map.entry("hour", Duration.ofHours(1)),
            Map.entry("60minute", Duration.ofHours(1)),
            Map.entry("30minute", Duration.ofMinutes(30)),
            Map.entry("15minute", Duration.ofMinutes(15)),
            Map.entry("10minute", Duration.ofMinutes(10)),
            Map.entry("5minute", Duration.ofMinutes(5)),
            Map.entry("3minute", Duration.ofMinutes(3)),
            Map.entry("minute", Duration.ofMinutes(1))
    );

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"); // Replace with your format
        List<ZonedDateTime> timestamps = req.timeStamp.stream()
                .map(ts -> Instant.ofEpochSecond(Long.parseLong(ts)).atZone(ZoneId.of("Asia/Kolkata")))
                .collect(Collectors.toList());
        BarSeries series = buildBarSeries(timestamps, req.open, req.high, req.low, req.close, req.volume, req.interval);
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
                MACDIndicator macd = new MACDIndicator(closePrice, req.getMacdShort(), req.getMacdLong());
                EMAIndicator macdSignal = new EMAIndicator(macd, req.getMacdSignal());

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

            case "VWAP":
                List<Double> vwapValues = new ArrayList<>();
                Num cumulativePV = series.numOf(0);
                Num cumulativeVolume = series.numOf(0);
                ZonedDateTime currentDay = null;

                for (int i = 0; i < series.getBarCount(); i++) {
                    Bar bar = series.getBar(i);
                    ZonedDateTime barDate = bar.getEndTime().withZoneSameInstant(ZoneId.of("Asia/Kolkata")).toLocalDate().atStartOfDay(ZoneId.of("Asia/Kolkata"));

                    if (!barDate.equals(currentDay)) {
                        // Reset for new day
                        cumulativePV = series.numOf(0);
                        cumulativeVolume = series.numOf(0);
                        currentDay = barDate;
                    }

                    Num typicalPrice = bar.getHighPrice().plus(bar.getLowPrice()).plus(bar.getClosePrice()).dividedBy(series.numOf(3));
                    Num pv = typicalPrice.multipliedBy(bar.getVolume());

                    cumulativePV = cumulativePV.plus(pv);
                    cumulativeVolume = cumulativeVolume.plus(bar.getVolume());

                    Num vwap = cumulativeVolume.isZero() ? series.numOf(0) : cumulativePV.dividedBy(cumulativeVolume);
                    vwapValues.add(vwap.doubleValue());
                }

                return new CustomIndicatorResponse(vwapValues);


            default:
                throw new IllegalArgumentException("Unknown indicator: " + req.indicator);
        }
    }

    public BarSeries buildBarSeries(List<ZonedDateTime> times,
                                    List<Double> open,
                                    List<Double> high,
                                    List<Double> low,
                                    List<Double> close,
                                    List<Double> volume,
                                    String interval) {

        String methodName = "buildBarSeries ";
        log.info(methodName + "entry");

        if (times.isEmpty()) {
            log.warn(methodName + "Input times list is empty. Returning empty BarSeries.");
            return new BaseBarSeriesBuilder().withName("empty_series").withNumTypeOf(DoubleNum::valueOf).build();
        }

        Duration barDuration = INTERVAL_DURATION_MAP.getOrDefault(interval.toLowerCase(), Duration.ofMinutes(1));
        if (!INTERVAL_DURATION_MAP.containsKey(interval.toLowerCase())) {
            log.warn(methodName + "Unknown interval: " + interval + ", defaulting to 1 minute");
        }

        BarSeries series = new BaseBarSeriesBuilder()
                .withName("custom_series")
                .withNumTypeOf(DoubleNum::valueOf)
                .build();

        for (int i = 0; i < times.size(); i++) {
            Num openNum = DoubleNum.valueOf(open.get(i));
            Num highNum = DoubleNum.valueOf(high.get(i));
            Num lowNum = DoubleNum.valueOf(low.get(i));
            Num closeNum = DoubleNum.valueOf(close.get(i));
            Num volumeNum = DoubleNum.valueOf(volume.get(i));
            Num amount = closeNum.multipliedBy(volumeNum);

            Bar bar = new BaseBar(
                    barDuration,
                    times.get(i),
                    openNum,
                    highNum,
                    lowNum,
                    closeNum,
                    volumeNum,
                    amount
            );
            series.addBar(bar);
        }

        log.info(methodName + "exit. Total bars added: {}", series.getBarCount());
        return series;
    }

    public int getSwingWindowSize(String interval) {
        String methodName = "getSwingWindowSize ";
        log.info(methodName + "entry");
        switch (interval) {
            case "3minute":
            case "5minute":
                return 2; // Intraday scalping – fast response
            case "10minute":
            case "15minute":
                return 3; // Short-term intraday
            case "30minute":
            case "60minute":
            case "hour":
                return 4; // Medium-term swing trades
            case "day":
                return 5; // Long-term position trades
            default:
                return 2; // Fallback for unknown intervals
        }
    }

    public SRDetectionParams getSRDetectionParams(String interval) {
        String methodName = "getSRDetectionParams ";
        log.info(methodName + "entry");
        log.debug(methodName + "interval: {}", interval);
        int lookBack, lookAhead;
        double thresholdPercent;

        switch (interval) {
            case "3minute":
            case "5minute":
                lookBack = lookAhead = 4;
                thresholdPercent = 0.0015;
                break;
            //Commented since default has same value.
            //Uncomment it if you update default values.
            /*case "10minute":
            case "15minute":
                lookBack = lookAhead = 5;
                thresholdPercent = 0.002;
                break;*/
            case "30minute":
            case "60minute":
            case "hour":
                lookBack = lookAhead = 6;
                thresholdPercent = 0.003;
                break;
            case "day":
                lookBack = lookAhead = 7;
                thresholdPercent = 0.005;
                break;
            default:
                lookBack = lookAhead = 5;
                thresholdPercent = 0.002;
                break;
        }
        log.info(methodName + "lookBack: {}, lookAhead: {}, thresholdPercent: {}", lookBack, lookAhead, thresholdPercent);
        return new SRDetectionParams(lookBack, lookAhead, thresholdPercent);
    }
}
