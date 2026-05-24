package com.gateway.api_gateway.controller;

import com.gateway.api_gateway.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/rate-limit-demo")
public class RateLimitDemoController {

    private final RateLimiterService rateLimiterService;

    public RateLimitDemoController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Value("${rate.limit.user.capacity:100}")
    private int userCapacity;

    @Value("${rate.limit.user.refill.interval:${rate.limit.user.refill:5}}")
    private int userRefillSeconds;

    @Value("${rate.limit.user.refill.amount:3}")
    private int userRefillAmount;

    @GetMapping("/config")
    public Map<String, Integer> getConfig() {
        return Map.of(
                "capacity", userCapacity,
                "refillSeconds", userRefillSeconds,
                "refillAmount", userRefillAmount
        );
    }

    @GetMapping("/state")
    public Map<String, Object> getState(HttpServletRequest request) {
        String userId = resolveUserId(request);
        String userKey = "rate:user:" + userId;
        RateLimiterService.BucketState state = rateLimiterService.getBucketState(
                userKey,
                userCapacity,
                userRefillSeconds,
                userRefillAmount
        );

        return Map.of(
                "key", userKey,
                "capacity", state.capacity(),
                "availableTokens", state.availableTokens(),
                "refillSeconds", state.refillIntervalSeconds(),
                "refillAmount", state.refillAmount(),
                "secondsUntilNextRefill", state.secondsUntilNextRefill()
        );
    }

    @PostMapping("/reset")
    public Map<String, Object> reset(HttpServletRequest request) {
        String userId = resolveUserId(request);
        String userKey = "rate:user:" + userId;
        rateLimiterService.resetBucket(userKey, userCapacity);

        return Map.of(
                "status", "reset",
                "key", userKey,
                "capacity", userCapacity
        );
    }

    @PostMapping("/audit/request")
    public Map<String, Object> auditRequest() {
        return Map.of(
                "status", "logged",
                "event", "request"
        );
    }

    @PostMapping("/audit/reset")
    public Map<String, Object> auditReset() {
        return Map.of(
                "status", "logged",
                "event", "reset"
        );
    }

    private String resolveUserId(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                userId = authentication.getName();
            }
        }
        return userId != null ? userId : "anonymous";
    }
}
