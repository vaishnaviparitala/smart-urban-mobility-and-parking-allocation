package com.smartcity.allocation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/allocate")
public class AllocationController {

    private final ParkingClient parkingClient;
    private final BookingClient bookingClient;

    public AllocationController(ParkingClient parkingClient, BookingClient bookingClient) {
        this.parkingClient = parkingClient;
        this.bookingClient = bookingClient;
    }

    /**
     * Automatic slot allocation:
     * 1. ask parking-service for FREE slots of the vehicle type, nearest entrance first
     * 2. try to reserve the nearest one (atomic - returns false if someone else took it)
     * 3. create the booking; if that fails, undo the reservation
     */
    @PostMapping
    public BookingDto allocate(@RequestHeader("X-User") String username,
                               @RequestBody AllocationRequest req) {

        if (req.vehicleType() == null || req.vehicleType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vehicleType is required (CAR or BIKE)");
        }
        String vehicleType = req.vehicleType().toUpperCase();

        List<SlotDto> freeSlots;
        try {
            freeSlots = parkingClient.slots("FREE", vehicleType);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Parking service is not reachable");
        }

        for (SlotDto slot : freeSlots) {
            if (parkingClient.reserve(slot.id())) {
                try {
                    return bookingClient.create(new BookingRequest(username, slot.id(), slot.slotNumber()));
                } catch (Exception e) {
                    parkingClient.release(slot.id());   // roll back so the slot is not lost
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Booking failed, please retry");
                }
            }
            // reserve() returned false: another driver got this slot first, try the next one
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No free slot available for " + vehicleType);
    }
}
