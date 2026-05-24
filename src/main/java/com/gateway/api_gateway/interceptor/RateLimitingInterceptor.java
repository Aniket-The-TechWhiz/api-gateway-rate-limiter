package com.gateway.api_gateway.interceptor;

import com.gateway.api_gateway.model.ApiRequestLog;
import com.gateway.api_gateway.service.LoggingService;
import com.gateway.api_gateway.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingInterceptor.class);
    private static final String BLOCKED_BY_RATE_LIMITER_ATTRIBUTE = "blockedByRateLimiter";

    private final RateLimiterService rateLimiterService;
    private final LoggingService loggingService;

    @Value("${rate.limit.user.capacity:100}")
    private int userCapacity;
    @Value("${rate.limit.user.refill.interval:${rate.limit.user.refill:5}}")
    private int userRefillInterval;
    @Value("${rate.limit.user.refill.amount:3}")
    private int userRefillAmount;
    @Value("${rate.limit.ip.capacity:50}")
    private int ipCapacity;
    @Value("${rate.limit.ip.refill.interval:${rate.limit.ip.refill:10}}")
    private int ipRefillInterval;
    @Value("${rate.limit.ip.refill.amount:3}")
    private int ipRefillAmount;

    public RateLimitingInterceptor(RateLimiterService rateLimiterService, LoggingService loggingService) {
        this.rateLimiterService = rateLimiterService;
        this.loggingService = loggingService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = (String) request.getAttribute("userId"); // may be set in JWT filter
        if (userId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                userId = authentication.getName();
            }
        }
        if (userId == null) userId = "anonymous";

        String clientIp = getClientIp(request);

        // User-level rate limit
        String userKey = "rate:user:" + userId;
        if (!rateLimiterService.allowRequest(userKey, userCapacity, userRefillInterval, userRefillAmount)) {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded for user");
            persistBlockedRequest(request, response, userId, "user");
            return false;
        }

        // IP-level throttling
        String ipKey = "rate:ip:" + clientIp;
        if (!rateLimiterService.allowRequest(ipKey, ipCapacity, ipRefillInterval, ipRefillAmount)) {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded for IP");
            persistBlockedRequest(request, response, userId, "ip");
            return false;
        }

        return true;
    }

    private void persistBlockedRequest(HttpServletRequest request, HttpServletResponse response, String userId, String reason) {
        request.setAttribute(BLOCKED_BY_RATE_LIMITER_ATTRIBUTE, Boolean.TRUE);

        ApiRequestLog log = new ApiRequestLog();
        log.setUserId(userId);
        log.setIpAddress(getClientIp(request));
        log.setEndpoint(request.getRequestURI());
        log.setHttpMethod(request.getMethod());
        log.setResponseStatus(response.getStatus());

        Object startTimeAttribute = request.getAttribute("logStartTime");
        long startTime = startTimeAttribute instanceof Long ? (Long) startTimeAttribute : System.currentTimeMillis();
        log.setResponseTimeMs(Math.max(0, System.currentTimeMillis() - startTime));
        log.setTimestamp(LocalDateTime.now());

        try {
            loggingService.saveLog(log);
        } catch (RuntimeException ex) {
            logger.warn("Failed to persist blocked {} request for {} {}", reason, log.getHttpMethod(), log.getEndpoint(), ex);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) return xf.split(",")[0];
        return request.getRemoteAddr();
    }
}
