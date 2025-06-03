package com.example.algotrading.controller;

import com.example.algotrading.data.entity.Equity;
import com.example.algotrading.data.entity.Exchange;
import com.example.algotrading.data.entity.Segment;
import com.example.algotrading.service.AlgoService;
import com.example.algotrading.service.UserTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class HistoricalDataController {

    private final AlgoService algoService;

    @Autowired
    public HistoricalDataController(UserTokenService userTokenService, AlgoService algoService) {
        this.algoService = algoService;
    }

    @GetMapping("/historical/init")
    @ResponseBody
    public Map<String, List<String>> getSegmentsAndExchanges() {
        String methodName = "getSegmentsAndExchanges ";
        log.info(methodName + "entry");
        List<String> segments = algoService.getAllSegments().stream().map(Segment::getName).collect(Collectors.toList());
        List<String> exchanges = algoService.getAllExchanges().stream().map(Exchange::getName).collect(Collectors.toList());
        Map<String, List<String>> result = new HashMap<>();
        result.put("segments", segments);
        result.put("exchanges", exchanges);
        log.debug(methodName + "segments fetched size {}, exchange fetched size {}", segments.size(), exchanges.size());
        log.info(methodName + "exit");
        return result;
    }

    @GetMapping("/historical/equities")
    @ResponseBody
    public Page<Equity> getEquities(
            @RequestParam String segment,
            @RequestParam String exchange,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        String methodName = "getEquities ";
        log.info(methodName + "entry");
        log.debug(methodName + "segment {}, exchange {}, search {}, page {}, size {}", segment, exchange, search, page, size);
        return algoService.findEquities(segment, exchange, search, PageRequest.of(page, size));
    }

    /*@PostMapping("/historical/data")
    @ResponseBody
    public List<HistoricalData> getHistoricalData(
            @RequestParam String equityName,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return historicalDataService.fetchData(equityName, startDate, endDate);
    }*/


}
