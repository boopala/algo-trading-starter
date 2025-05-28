package com.example.algotrading.controller;

import com.example.algotrading.batch.EquityBatchScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class KiteBatchController {

    @Autowired
    private EquityBatchScheduler equityBatchScheduler;

    @PostMapping("/fetchInstruments")
    public String runBatchJob() {
        try {
            equityBatchScheduler.fetchKiteInstruments();
            return "Fetch is successful";
        } catch (Exception e) {
            return "Fetch failed: " + e.getMessage();
        }
    }

}
