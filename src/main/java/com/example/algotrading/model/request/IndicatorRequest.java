package com.example.algotrading.model.request;

import lombok.Data;

import java.util.List;

@Data
public class IndicatorRequest {
    public List<Double> open;
    public List<Double> high;
    public List<Double> low;
    public List<Double> close;
    public List<Double> volume;
}
