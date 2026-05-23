package com.gateway.api_gateway.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "api_request_log")
public class ApiRequestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String ipAddress;
    private String endpoint;
    private String httpMethod;
    private int responseStatus;
    private long responseTimeMs;
    private LocalDateTime timestamp;
}