package com.example.algotrading.controller;

import com.example.algotrading.service.EncryptionService;
import com.example.algotrading.service.KiteService;
import com.example.algotrading.service.UserTokenService;
import com.example.algotrading.util.CommonUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import com.zerodhatech.models.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@Slf4j
public class AlgoDashboardController {

    private final KiteService kiteService;
    private final UserTokenService userTokenService;
    private final EncryptionService encryptionService;

    @Autowired
    public AlgoDashboardController(KiteService kiteService, UserTokenService userTokenService, EncryptionService encryptionService) {
        this.kiteService = kiteService;
        this.userTokenService = userTokenService;
        this.encryptionService = encryptionService;
    }

    @GetMapping("/holdings")
    public String getHoldings(HttpSession session, Model model) throws IOException, KiteException {
        String methodName = "getHoldings ";
        log.info(methodName + "entry");
        String userId = (String) session.getAttribute("user_id");
        if (Optional.ofNullable(userId).isPresent()) {
            Optional<String> encryptedToken = userTokenService.getAccessTokenByUserId(userId);
            if (encryptedToken.isEmpty()) {
                log.info(methodName + "Access token unavailable for userId");
                return "redirect:/login";
            }
            String accessToken = encryptionService.decrypt(encryptedToken.get());
            List<Holding> holdings = kiteService.getHoldings(accessToken, userId);
            CommonUtil.setAdditionalAttributes(model, holdings);
            model.addAttribute("holdings", holdings);
            log.info(methodName + "holding fetched size {}", holdings.size());
            log.info(methodName + "exit");
            return "fragments/holdings :: holdingsPanel";
        } else {
            log.info(methodName + "Access token unavailable for userId");
            return "redirect:/login";
        }
    }

    @GetMapping("/positions")
    public String getPositions(HttpSession session, Model model) throws IOException, KiteException {
        String methodName = "getPositions ";
        log.info(methodName + "entry");
        String userId = (String) session.getAttribute("user_id");
        Optional<String> encryptedToken = userTokenService.getAccessTokenByUserId(userId);
        if (encryptedToken.isEmpty()) {
            log.info(methodName + "Access token unavailable for userId");
            return "redirect:/login";
        }
        String accessToken = encryptionService.decrypt(encryptedToken.get());
        Map<String, List<Position>> positionMap = kiteService.getPositions(accessToken, userId);
        model.addAttribute("positions", positionMap.get("net"));
        log.info(methodName + "position fetched size {}", positionMap.get("net").size());
        log.info(methodName + "exit");
        return "fragments/positions :: positionsPanel";
    }

}
