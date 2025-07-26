package com.example.algotrading.rest;

import com.example.algotrading.batch.EquityBatchScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class KiteBatchController {

    @Autowired
    private EquityBatchScheduler equityBatchScheduler;

    @PostMapping("/fetchInstruments")
    public String runBatchJob() {
        String methodName = "runBatchJob ";
        log.info(methodName + "entry");
        String message;
        try {
            equityBatchScheduler.fetchKiteInstruments();
            message = "Fetch is successful";
        } catch (Exception e) {
            log.error(methodName + "Exception occurred ", e);
            message = "Fetch failed: Check log for more info";
        }
        log.info(methodName + "exit");
        return message;
    }

    @PostMapping("/updateEquityExpiry")
    public String updateEquityExpiry() {
        String methodName = "updateEquityExpiry ";
        log.info(methodName + "entry");
        String message;
        try {
            equityBatchScheduler.updateEquityExpiry();
            message = "Expiry update is successful";
        } catch (Exception e) {
            log.error(methodName + "Exception occurred ", e);
            message = "Expiry update failed: Check log for more info";
        }
        log.info(methodName + "exit");
        return message;
    }

}
