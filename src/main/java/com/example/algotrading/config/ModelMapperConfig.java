package com.example.algotrading.config;

import com.zerodhatech.models.Holding;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        // Converter for Date to LocalDateTime
        Converter<Date, LocalDateTime> dateToLocalDateTime = ctx ->
                ctx.getSource() == null ? null : ctx.getSource().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        modelMapper.addMappings(new PropertyMap<Holding, com.example.algotrading.data.entity.Holding>() {
            @Override
            protected void configure() {
                // Map snake_case to camelCase
                map().setTradingSymbol(source.tradingSymbol);
                map().setT1Quantity(source.t1Quantity);
                map().setCollateralQuantity(source.collateralQuantity);
                map().setCollateraltype(source.collateraltype);
                map().setIsin(source.isin);
                map().setPnl(source.pnl);
                map().setQuantity(source.quantity);
                map().setRealisedQuantity(source.realisedQuantity);
                map().setAveragePrice(source.averagePrice);
                map().setExchange(source.exchange);
                map().setInstrumentToken(source.instrumentToken);
                map().setUsedQuantity(source.usedQuantity);
                map().setAuthorisedQuantity(source.authorisedQuantity);
                map().setDiscrepancy(source.discrepancy);
                map().setDayChange(source.dayChange);
                map().setDayChangePercentage(source.dayChangePercentage);
                map().setProduct(source.product);
                map().setLastPrice(source.lastPrice);
                map().setPrice(source.price);

                // Date to LocalDateTime
                using(dateToLocalDateTime).map(source.authorisedDate).setAuthorisedDate(null);
            }
        });

        return modelMapper;
    }
}

