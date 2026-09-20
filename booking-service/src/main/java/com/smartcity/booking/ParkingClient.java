package com.smartcity.booking;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/** Calls parking-service by its Eureka name (no hard-coded host or port). */
@FeignClient(name = "parking-service")
public interface ParkingClient {

    @PutMapping("/internal/parking/slots/{id}/release")
    boolean release(@PathVariable("id") Long id);
}
