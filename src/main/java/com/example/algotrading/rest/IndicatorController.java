package com.example.algotrading.rest;

import com.example.algotrading.model.request.CustomIndicatorRequest;
import com.example.algotrading.model.request.IndicatorRequest;
import com.example.algotrading.model.response.CustomIndicatorResponse;
import com.example.algotrading.model.response.IndicatorResponse;
import com.example.algotrading.service.IndicatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indicators")
@Slf4j
public class IndicatorController {

    @Autowired
    private IndicatorService indicatorService;

    @PostMapping("/calculateIndicators")
    public IndicatorResponse calculateIndicators(@RequestBody IndicatorRequest request) {
        String methodName = "calculateIndicators ";
        log.info(methodName + "entry");
        return indicatorService.calculateIndicators(
                request.open, request.high, request.low, request.close, request.volume
        );
    }

    @PostMapping("/customIndicator")
    public CustomIndicatorResponse getCustomIndicator(@RequestBody CustomIndicatorRequest request) {
        return indicatorService.calculateCustomIndicator(request);
    }
}
