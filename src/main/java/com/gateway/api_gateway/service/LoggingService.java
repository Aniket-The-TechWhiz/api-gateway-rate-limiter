package com.gateway.api_gateway.service;

import com.gateway.api_gateway.model.ApiRequestLog;
import com.gateway.api_gateway.repository.ApiRequestLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class LoggingService {

    @Autowired
    private ApiRequestLogRepository logRepository;

    @Async
    public void saveLog(ApiRequestLog log) {
        logRepository.save(log);
    }
}