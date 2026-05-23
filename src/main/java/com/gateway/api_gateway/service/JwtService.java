package com.gateway.api_gateway.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;


@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret="a-string-secret-at-least-256-bits-long";

    @Value("${jwt.expiration.ms}")
    private Long expirationMs;

    public Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(String Username){
        Date now=new Date();
        Date expiry=new Date(now.getTime()+expirationMs);
        return Jwts.builder()
                .subject(Username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith((SecretKey) key())
        .compact();
    }

    public String extractUsername(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token){
        try{
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(token);;
            return true;
        }catch (JwtException e){
            return false;
        }
    }
}
