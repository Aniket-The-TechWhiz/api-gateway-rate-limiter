package com.gateway.api_gateway.interceptor;

import com.gateway.api_gateway.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    @Value("${rate.limit.user.capacity:100}")
    private int userCapacity;
    @Value("${rate.limit.user.refill:20}")
    private int userRefill;
    @Value("${rate.limit.ip.capacity:50}")
    private int ipCapacity;
    @Value("${rate.limit.ip.refill:10}")
    private int ipRefill;

    public RateLimitingInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = (String) request.getAttribute("userId"); // set in JWT filter
        if (userId == null) userId = "anonymous";

        String clientIp = getClientIp(request);

        // User-level rate limit
        String userKey = "rate:user:" + userId;
        if (!rateLimiterService.allowRequest(userKey, userCapacity, userRefill)) {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded for user");
            return false;
        }

        // IP-level throttling
        String ipKey = "rate:ip:" + clientIp;
        if (!rateLimiterService.allowRequest(ipKey, ipCapacity, ipRefill)) {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded for IP");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) return xf.split(",")[0];
        return request.getRemoteAddr();
    }
}
