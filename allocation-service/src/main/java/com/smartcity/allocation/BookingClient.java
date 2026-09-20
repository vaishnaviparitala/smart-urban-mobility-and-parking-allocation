package com.smartcity.allocation;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "booking-service")
public interface BookingClient {

    @PostMapping("/internal/bookings")
    BookingDto create(@RequestBody BookingRequest request);
}
