package com.gateway.api_gateway.interceptor;

import com.gateway.api_gateway.model.ApiRequestLog;
import com.gateway.api_gateway.service.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Autowired
    private LoggingService loggingService;

    private long startTime;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long duration = System.currentTimeMillis() - startTime;
        ApiRequestLog log = new ApiRequestLog();
        log.setUserId((String) request.getAttribute("userId"));
        log.setIpAddress(request.getRemoteAddr());
        log.setEndpoint(request.getRequestURI());
        log.setHttpMethod(request.getMethod());
        log.setResponseStatus(response.getStatus());
        log.setResponseTimeMs(duration);
        log.setTimestamp(LocalDateTime.now());
        loggingService.saveLog(log);
    }
}
