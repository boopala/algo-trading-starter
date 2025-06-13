package com.example.algotrading.util;

import com.zerodhatech.models.Holding;
import org.modelmapper.ModelMapper;
import org.springframework.ui.Model;

import java.util.List;

public class CommonUtil {

    public static void setAdditionalAttributes(Model model, List<com.example.algotrading.model.Holding> holdings) {
        double totalInvested = 0;
        double totalCurrentValue = 0;
        double totalPnl = 0;
        double totalDayChange = 0;
        for (com.example.algotrading.model.Holding h : holdings) {
            totalInvested += h.getQuantity() * h.getAveragePrice();
            totalCurrentValue += h.getQuantity() * h.getLastPrice();
            totalPnl += h.getPnl();
            totalDayChange += h.getDayChange() * h.getQuantity(); // Or compute per holding * qty
        }
        model.addAttribute("totalInvested", totalInvested);
        model.addAttribute("totalCurrentValue", totalCurrentValue);
        model.addAttribute("totalPnl", totalPnl);
        model.addAttribute("totalDayChange", totalDayChange);
    }

    public static com.example.algotrading.model.Holding convertHoldingEntityToDto(com.example.algotrading.data.entity.Holding entity, ModelMapper modelMapper) {
        return modelMapper.map(entity, com.example.algotrading.model.Holding.class);
    }

    public static com.example.algotrading.data.entity.Holding convertHoldingDTOToEntity(com.example.algotrading.model.Holding dto, ModelMapper modelMapper) {
        return modelMapper.map(dto, com.example.algotrading.data.entity.Holding.class);
    }

    public static com.example.algotrading.model.Holding convertHoldingZerodhaToHoldingDTO(Holding holding, ModelMapper modelMapper) {
        return modelMapper.map(holding, com.example.algotrading.model.Holding.class);
    }
}
