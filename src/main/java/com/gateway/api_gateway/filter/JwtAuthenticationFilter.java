package com.gateway.api_gateway.filter;

import com.gateway.api_gateway.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path=request.getRequestURI();
        boolean publicPath = path.equals("/auth/login") ||
                path.equals("/dashboard") ||
                path.startsWith("/api/analytics") ||
            path.startsWith("/api/rate-limit-demo") ||
                path.startsWith("/css") ||
                path.startsWith("/js");

        String authHeader=request.getHeader("Authorization");
        // Expect header of form: "Bearer <token>" — require the space to avoid substring errors
        if(authHeader==null || !authHeader.startsWith("Bearer ")){
            if(publicPath){
                filterChain.doFilter(request,response);
                return;
            }
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Missing or Invalid Token");
            return;
        }

        String token=authHeader.substring(7);
        if(!jwtService.validateToken(token)){
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Invalid Token");
            return;
        }

        String username=jwtService.extractUsername(token);
        request.setAttribute("userId", username);
        UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken(username,null, Collections.emptyList());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request,response);
    }
}
