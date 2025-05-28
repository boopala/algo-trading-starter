package com.example.algotrading.controller;

import com.example.algotrading.model.response.*;
import com.example.algotrading.service.EncryptionService;
import com.example.algotrading.service.KiteService;
import com.example.algotrading.service.UserTokenService;
import com.example.algotrading.util.CommonUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import com.zerodhatech.models.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
public class KiteAuthController {

    private final KiteService kiteService;
    private final UserTokenService userTokenService;
    private final EncryptionService encryptionService;

    @Autowired
    public KiteAuthController(KiteService kiteService, UserTokenService userTokenService, EncryptionService encryptionService) {
        this.kiteService = kiteService;
        this.userTokenService = userTokenService;
        this.encryptionService = encryptionService;
    }

    @GetMapping("/login")
    public RedirectView kiteLogin() throws KiteException {
        return new RedirectView(kiteService.generateLoginUrl());
    }

    @GetMapping("/login/callback")
    public String kiteCallback(@RequestParam("request_token") String requestToken, HttpSession session) throws KiteException {
        try {
            TokenResponse tokenResponse = kiteService.generateAccessToken(requestToken);
            session.setAttribute("user_id", tokenResponse.getUserId());
            return "redirect:/dashboard";

        } catch (Exception e) {
            session.setAttribute("message", "Failed to generate token: " + e.getMessage());
        }
        return "redirect:/dashboard"; // thymeleaf template
    }

    @GetMapping("/dashboard")
    public String getDashboard(HttpSession session, Model model) throws Exception, KiteException {
        String userId = (String) session.getAttribute("user_id");

        Optional<String> encryptedToken = userTokenService.getAccessTokenByUserId(userId);
        if (encryptedToken.isEmpty()) {
            return "redirect:/login"; // Or show error
        }

        String accessToken = encryptionService.decrypt(encryptedToken.get());
        Profile profile = kiteService.getUserProfile(accessToken, userId);
        List<Holding> holdings = kiteService.getHoldings(accessToken, userId);
        CommonUtil.setAdditionalAttributes(model, holdings);
        model.addAttribute("profile", profile);
        model.addAttribute("holdings", holdings);

        return "index";
    }


}
