package com.example.algotrading.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class HistoricalData extends com.zerodhatech.models.HistoricalData {

    private boolean resistance = false;
    private boolean support = false;

}
