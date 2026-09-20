package com.smartcity.gateway;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Answer returned when a downstream service is down or too slow (circuit breaker fallback). */
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public ResponseEntity<Map<String, String>> fallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service is temporarily unavailable, please try again shortly"));
    }
}
