package com.gateway.api_gateway.service;

import com.gateway.api_gateway.model.ApiRequestLog;
import com.gateway.api_gateway.repository.ApiRequestLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoggingService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);

    private final ApiRequestLogRepository logRepository;

    public LoggingService(ApiRequestLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional
    public void saveLog(ApiRequestLog log) {
        try {
            logRepository.saveAndFlush(log);
        } catch (RuntimeException ex) {
            logger.warn("Failed to persist API request log for {} {}", log.getHttpMethod(), log.getEndpoint(), ex);
            throw ex;
        }
    }
}