package com.example.algotrading.service;

import com.example.algotrading.model.response.HistoricalData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
