package com.example.algotrading.controller;

import com.example.algotrading.model.Holding;
import com.example.algotrading.model.response.*;
import com.example.algotrading.service.EncryptionService;
import com.example.algotrading.service.KiteService;
import com.example.algotrading.service.KiteWebSocketService;
import com.example.algotrading.service.UserTokenService;
import com.example.algotrading.util.CommonUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Position;
import com.zerodhatech.models.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class KiteAuthController {

    private final KiteService kiteService;
    private final UserTokenService userTokenService;
    private final EncryptionService encryptionService;
    private final KiteWebSocketService kiteWebSocketService;

    @Autowired
    public KiteAuthController(KiteService kiteService, UserTokenService userTokenService, EncryptionService encryptionService, @Autowired(required = false) KiteWebSocketService kiteWebSocketService) {
        this.kiteService = kiteService;
        this.userTokenService = userTokenService;
        this.encryptionService = encryptionService;
        this.kiteWebSocketService = kiteWebSocketService;
    }

    @GetMapping("/login")
    public RedirectView kiteLogin() throws KiteException {
        String methodName = "kiteLogin ";
        log.info(methodName + "entry");
        String loginUrl = kiteService.generateLoginUrl();
        log.debug(methodName + "loginUrl: {}", loginUrl);
        return new RedirectView(loginUrl);
    }

    @GetMapping("/login/callback")
    public String kiteCallback(@RequestParam("request_token") String requestToken, HttpSession session) throws KiteException {
        String methodName = "kiteCallback ";
        log.info(methodName + "entry");
        try {
            TokenResponse tokenResponse = kiteService.generateAccessToken(requestToken);
            session.setAttribute("user_id", tokenResponse.getUserId());
            log.debug(methodName + "access token generated from kite successfully");
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error(methodName + "exception occurred", e);
            session.setAttribute("message", "Failed to generate token: " + e.getMessage());
        }
        log.info(methodName + "exit");
        return "redirect:/dashboard"; // thymeleaf template
    }

    @GetMapping("/dashboard")
    public String getDashboard(HttpSession session, Model model) throws Exception, KiteException {
        String methodName = "getDashboard ";
        log.info(methodName + "entry");
        String userId = (String) session.getAttribute("user_id");
        Optional<String> encryptedToken = userTokenService.getAccessTokenByUserId(userId);
        if (encryptedToken.isEmpty()) {
            log.info(methodName + "Access token unavailable for userId");
            return "redirect:/login";
        }
        String accessToken = encryptionService.decrypt(encryptedToken.get());
        Profile profile = kiteService.getUserProfile(accessToken, userId);
        List<Holding> holdings = kiteService.getHoldings(accessToken, userId);
        if (kiteWebSocketService != null) {
            kiteWebSocketService.startWebSocket(accessToken, (ArrayList<Long>) holdings.stream().map(Holding::getInstrumentToken)
                    .map(Long::parseLong).collect(Collectors.toList()));
        }
        Map<String, List<Position>> positionMap = kiteService.getPositions(accessToken, userId);
        CommonUtil.setAdditionalAttributes(model, holdings);
        model.addAttribute("profile", profile);
        model.addAttribute("holdings", holdings);
        model.addAttribute("positions", positionMap.get("net"));
        log.info(methodName + "holding fetched size {}, position fetched size {}", holdings.size(), positionMap.get("net").size());
        log.info(methodName + "exit");
        return "index";
    }


}
