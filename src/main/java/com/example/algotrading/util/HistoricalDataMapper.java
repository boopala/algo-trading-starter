package com.example.algotrading.util;

import com.example.algotrading.model.response.HistoricalData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class HistoricalDataMapper {
    public static List<HistoricalData> mapFromKite(List<com.zerodhatech.models.HistoricalData> kiteCandles) {
        String methodName = "mapFromKite ";
        log.info(methodName + "entry");
        List<HistoricalData> result = new ArrayList<>();
        for (com.zerodhatech.models.HistoricalData candle : kiteCandles) {
            HistoricalData hd = new HistoricalData();
            hd.timeStamp = candle.timeStamp;
            hd.open = candle.open;
            hd.high = candle.high;
            hd.low = candle.low;
            hd.close = candle.close;
            hd.volume = candle.volume;
            hd.oi = candle.oi;
            result.add(hd);
        }
        log.info(methodName + "exit");
        return result;
    }}
