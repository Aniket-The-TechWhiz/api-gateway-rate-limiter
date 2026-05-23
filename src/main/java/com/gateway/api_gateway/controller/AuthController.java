package com.gateway.api_gateway.controller;

import com.gateway.api_gateway.dto.LoginRequest;
import com.gateway.api_gateway.dto.LoginResponse;
import com.gateway.api_gateway.service.JwtService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        // TODO: Validate username/password from database
        String token = jwtService.generateToken(req.getUsername());
        return new LoginResponse(token);
    }
}
