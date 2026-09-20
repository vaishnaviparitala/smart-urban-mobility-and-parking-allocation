package com.smartcity.gateway;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * Central security check. Every request except /auth/** must carry a valid JWT.
 * The verified identity is passed to the services as X-User and X-Role headers.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final SecretKey key;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Open endpoints: login/register, CORS pre-flight, gateway fallback
        if (path.startsWith("/auth/") || path.startsWith("/fallback")
                || HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        // 1. Token must be present
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        // 2. Signature and expiry must be valid
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(header.substring(7)).getPayload();
        } catch (Exception e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String role = claims.get("role", String.class);

        // 3. Role rule: only ADMIN may add or delete parking data
        boolean isWrite = !HttpMethod.GET.equals(request.getMethod());
        if (path.startsWith("/api/parking") && isWrite && !"ADMIN".equals(role)) {
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        // 4. Forward verified identity (this overwrites any X-User a client tried to fake)
        ServerHttpRequest mutated = request.mutate()
                .header("X-User", claims.getSubject())
                .header("X-Role", role == null ? "USER" : role)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
