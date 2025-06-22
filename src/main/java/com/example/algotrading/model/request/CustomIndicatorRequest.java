package com.example.algotrading.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomIndicatorRequest extends IndicatorRequest {
    public String indicator; // e.g., "SMA", "EMA", "RSI"
    public int period;
}
