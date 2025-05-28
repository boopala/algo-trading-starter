package com.example.algotrading.util;

import com.zerodhatech.models.Holding;
import org.springframework.ui.Model;

import java.util.List;

public class CommonUtil {

    public static void setAdditionalAttributes(Model model, List<Holding> holdings) {
        double totalInvested = 0;
        double totalCurrentValue = 0;
        double totalPnl = 0;
        double totalDayChange = 0;
        for (Holding h : holdings) {
            totalInvested += h.quantity * h.averagePrice;
            totalCurrentValue += h.quantity * h.lastPrice;
            totalPnl += h.pnl;
            totalDayChange += h.dayChange * h.quantity; // Or compute per holding * qty
        }
        model.addAttribute("totalInvested", totalInvested);
        model.addAttribute("totalCurrentValue", totalCurrentValue);
        model.addAttribute("totalPnl", totalPnl);
        model.addAttribute("totalDayChange", totalDayChange);
    }
}
