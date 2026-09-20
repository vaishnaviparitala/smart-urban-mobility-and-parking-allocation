package com.smartcity.allocation;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "parking-service")
public interface ParkingClient {

    @GetMapping("/api/parking/slots")
    List<SlotDto> slots(@RequestParam("status") String status,
                        @RequestParam("vehicleType") String vehicleType);

    @PutMapping("/internal/parking/slots/{id}/reserve")
    boolean reserve(@PathVariable("id") Long id);

    @PutMapping("/internal/parking/slots/{id}/release")
    boolean release(@PathVariable("id") Long id);
}
