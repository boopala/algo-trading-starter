package com.example.algotrading.service;

import com.example.algotrading.data.repository.HoldingRepository;
import com.zerodhatech.models.Holding;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HoldingsService {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public List<com.example.algotrading.data.entity.Holding> saveKiteHoldings(List<Holding> kiteHoldings) {
        return kiteHoldings.stream().map(this::saveIfNotExistsOrDeleted).collect(Collectors.toList());
    }

    private com.example.algotrading.data.entity.Holding saveIfNotExistsOrDeleted(Holding holding) {
        Optional<com.example.algotrading.data.entity.Holding> existingOpt = holdingRepository.findByIsinAndIsDeletedFalse(holding.isin);

        if (existingOpt.isPresent()) {
            com.example.algotrading.data.entity.Holding existing = existingOpt.get();
            // Update fields of existing entity as needed
            existing.setQuantity(holding.quantity);
            existing.setAveragePrice(holding.averagePrice);
            existing.setLastPrice(holding.lastPrice);
            existing.setPnl(holding.pnl);
            existing.setDayChange(holding.dayChange);
            existing.setExchange(holding.exchange);
            existing.setInstrumentToken(holding.instrumentToken);
            existing.setAuthorisedDate(holding.authorisedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            existing.setDayChangePercentage(holding.dayChangePercentage);
            existing.setUpdatedAt(LocalDateTime.now());
            // ... update other fields as needed
            return holdingRepository.saveAndFlush(existing);
        } else {
            // Insert new record (either no active record or only is_deleted=true records exist)
            com.example.algotrading.data.entity.Holding entity = modelMapper.map(holding, com.example.algotrading.data.entity.Holding.class);
            entity.setCreatedAt(LocalDateTime.now());    // set created_at
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setIsDeleted(false);
            return holdingRepository.saveAndFlush(entity);
        }
    }

    public List<com.example.algotrading.data.entity.Holding> findAllIsDeletedFalse() {
        return holdingRepository.findAllByIsDeletedFalse();
    }

    @Transactional
    public void updateHolding(com.example.algotrading.data.entity.Holding holdingEntity) {
        holdingRepository.saveAndFlush(holdingEntity);
    }

}
