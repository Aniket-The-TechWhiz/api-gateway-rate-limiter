package com.gateway.api_gateway.interceptor;

import com.gateway.api_gateway.model.ApiRequestLog;
import com.gateway.api_gateway.service.JwtService;
import com.gateway.api_gateway.service.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String BLOCKED_BY_RATE_LIMITER_ATTRIBUTE = "blockedByRateLimiter";

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("logStartTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (Boolean.TRUE.equals(request.getAttribute(BLOCKED_BY_RATE_LIMITER_ATTRIBUTE))) {
            return;
        }

        if (!isSuccessfulResponse(response.getStatus())) {
            return;
        }

        Object startTimeAttribute = request.getAttribute("logStartTime");
        long startTime = startTimeAttribute instanceof Long ? (Long) startTimeAttribute : System.currentTimeMillis();
        long duration = System.currentTimeMillis() - startTime;
        ApiRequestLog log = new ApiRequestLog();
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                userId = authentication.getName();
            }
        }
        if (userId == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtService.validateToken(token)) {
                    userId = jwtService.extractUsername(token);
                }
            }
        }
        if (userId == null) {
            userId = "anonymous";
        }
        log.setUserId(userId);
        log.setIpAddress(request.getRemoteAddr());
        log.setEndpoint(request.getRequestURI());
        log.setHttpMethod(request.getMethod());
        log.setResponseStatus(response.getStatus());
        log.setResponseTimeMs(duration);
        log.setTimestamp(LocalDateTime.now());
        loggingService.saveLog(log);
    }

    private boolean isSuccessfulResponse(int status) {
        return status >= 200 && status < 300;
    }
}
