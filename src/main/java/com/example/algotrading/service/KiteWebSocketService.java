package com.example.algotrading.service;

import com.example.algotrading.model.Holding;
import com.example.algotrading.util.CommonUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnError;
import com.zerodhatech.ticker.OnTicks;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "live.holdings.websocket", name = "enabled", havingValue = "true")
public class KiteWebSocketService {

    private KiteTicker tickerProvider;

    @Value("${kite.api-key}")
    private String apiKey;

    @Value("${kite.user-id}")
    private String userId;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final KiteService kiteService;

    @Autowired
    private HoldingsService holdingsService;

    @Autowired
    private ModelMapper modelMapper;

    public KiteWebSocketService(KiteService kiteService) {
        this.kiteService = kiteService;
    }

    public void startWebSocket(String accessToken, ArrayList<Long> tokens) throws KiteException {
        String methodName = "startWebSocket ";
        log.info(methodName + "entry");
        /*KiteConnect kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setAccessToken(accessToken);*/
        log.debug(methodName + "accessToken: {}", accessToken);
        log.debug(methodName + "apiKey: {}", apiKey);
        tickerProvider = new KiteTicker(accessToken, apiKey);
        boolean isConnected = tickerProvider.isConnectionOpen();
        log.info(methodName + "before disconnect isConnected: {}", isConnected);
        tickerProvider.disconnect();
        log.info(methodName + "after disconnect isConnected: {}", isConnected);
        tickerProvider.setOnConnectedListener(new OnConnect() {
            @Override
            public void onConnected() {
                log.info(methodName + "onConnected entry");
                tickerProvider.subscribe(tokens);
                tickerProvider.setMode(tokens, KiteTicker.modeFull);
                log.info(methodName + "onConnected exit");
            }
        });

        tickerProvider.setOnTickerArrivalListener(new OnTicks() {
            @Override
            public void onTicks(ArrayList<Tick> ticks) {
                log.info(methodName + "onTicks entry");
                // Update your holdings data here, e.g., update in-memory map or DB
                try {
                    updateHoldingsWithTicks(ticks, accessToken);
                } catch (IOException e) {
                    log.error(methodName + "setOnTickerArrivalListener IOException occurred ", e);
                    throw new RuntimeException(e);
                } catch (KiteException e) {
                    log.error(methodName + "setOnTickerArrivalListener KiteException occurred ", e);
                    throw new RuntimeException(e);
                }
                log.info(methodName + "onTicks exit");
                // Optionally: Notify frontend via Spring WebSocket (see below)
            }
        });

        tickerProvider.setOnErrorListener(new OnError() {
            @Override
            public void onError(Exception e) {
                log.error(methodName + "setOnErrorListener Exception occurred ", e);
            }

            @Override
            public void onError(KiteException e) {
                log.error(methodName + "setOnErrorListener KiteException occurred ", e);
            }

            @Override
            public void onError(String s) {
                log.error(methodName + "setOnErrorListener errorMessage: {}", s);
            }
        });

        tickerProvider.setTryReconnection(true);
        tickerProvider.setMaximumRetries(10);
        tickerProvider.setMaximumRetryInterval(30);
        tickerProvider.connect();
        //tickerProvider.unsubscribe(tokens);

        // After using com.zerodhatech.com.zerodhatech.ticker, close websocket connection.
        //tickerProvider.disconnect();
        log.info(methodName + "exit");
    }

    private void updateHoldingsWithTicks(List<Tick> ticks, String accessToken) throws IOException, KiteException {
        String methodName = "updateHoldingsWithTicks ";
        log.info(methodName + "entry");
        // ... update holdings logic ...
        // After updating, send updated holdings to clients:
        List<com.example.algotrading.data.entity.Holding> holdingEntities = holdingsService.findAllIsDeletedFalse();
        // Create a lookup map from instrumentToken to Holding
        Map<String, com.example.algotrading.data.entity.Holding> holdingMap = holdingEntities.stream()
                .filter(h -> h.getInstrumentToken() != null)
                .collect(Collectors.toMap(com.example.algotrading.data.entity.Holding::getInstrumentToken, Function.identity()));

        // Update lastPrice efficiently
        ticks.forEach(tick -> {
            String token = String.valueOf(tick.getInstrumentToken());
            com.example.algotrading.data.entity.Holding holding = holdingMap.get(token);
            if (holding != null) {
                holding.setLastPrice(tick.getLastTradedPrice());
                double currentValue = holding.getQuantity() * holding.getLastPrice();
                double invested = holding.getQuantity() * holding.getAveragePrice();
                holding.setPnl(currentValue - invested);
                double dayChange = tick.getLastTradedPrice() - tick.getClosePrice();
                holding.setDayChange(dayChange);
                holding.setUpdatedAt(LocalDateTime.now());
                holdingsService.updateHolding(holding);
            }
        });
        List<com.example.algotrading.data.entity.Holding> updatedHoldings = new ArrayList<>(holdingMap.values());

        // Convert to DTOs
        List<Holding> holdings = updatedHoldings.stream()
                .map(h -> CommonUtil.convertHoldingEntityToDto(h, modelMapper))
                .collect(Collectors.toList());
        log.info(methodName + "exit");
        messagingTemplate.convertAndSend("/topic/holdings", holdings);
    }
}
