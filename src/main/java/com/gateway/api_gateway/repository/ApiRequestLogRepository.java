package com.gateway.api_gateway.repository;

import com.gateway.api_gateway.model.ApiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, Long> {
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<ApiRequestLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    @Query("SELECT l.userId, COUNT(l) FROM ApiRequestLog l WHERE l.timestamp BETWEEN :start AND :end GROUP BY l.userId ORDER BY COUNT(l) DESC")
    List<Object[]> findTopUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
