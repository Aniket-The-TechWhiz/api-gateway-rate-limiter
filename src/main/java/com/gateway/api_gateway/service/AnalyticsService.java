package com.gateway.api_gateway.service;

import com.gateway.api_gateway.repository.ApiRequestLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final ApiRequestLogRepository logRepository;

    public AnalyticsService(ApiRequestLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public Map<String, Object> getSummary(LocalDateTime start, LocalDateTime end) {
        long totalRequests = logRepository.countByTimestampBetween(start, end);
        List<Object[]> topUsers = logRepository.findTopUsers(start, end);
        Map<String, Object> result = new HashMap<>();
        result.put("totalRequests", totalRequests);
        result.put("topUsers", topUsers);
        return result;
    }
}
