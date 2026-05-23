package com.gateway.api_gateway.controller;

import com.gateway.api_gateway.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary(@RequestParam(defaultValue = "24") int hours) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hours);
        return analyticsService.getSummary(start, end);
    }
}