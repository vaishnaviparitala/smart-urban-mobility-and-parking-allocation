package com.smartcity.gateway;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/** Simple fixed-window rate limiter: max N requests per minute per client IP. */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final long WINDOW_MS = 60_000;

    private final int maxRequestsPerMinute;
    private final Map<String, long[]> counters = new ConcurrentHashMap<>();   // ip -> [windowStart, count]

    public RateLimitFilter(@Value("${gateway.rate-limit.max-requests-per-minute:100}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        String ip = (remote != null && remote.getAddress() != null)
                ? remote.getAddress().getHostAddress() : "unknown";

        long now = System.currentTimeMillis();
        long[] window = counters.compute(ip, (k, v) -> {
            if (v == null || now - v[0] >= WINDOW_MS) {
                return new long[] { now, 1 };
            }
            v[1]++;
            return v;
        });

        if (window[1] > maxRequestsPerMinute) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
