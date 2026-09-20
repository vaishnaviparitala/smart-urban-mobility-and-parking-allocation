package com.smartcity.booking;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BookingController {

    private final BookingRepository repo;
    private final ParkingClient parkingClient;
    private final double ratePerHour;

    public BookingController(BookingRepository repo,
                             ParkingClient parkingClient,
                             @Value("${parking.rate-per-hour:20}") double ratePerHour) {
        this.repo = repo;
        this.parkingClient = parkingClient;
        this.ratePerHour = ratePerHour;
    }

    // ---------- Internal: called by allocation-service (no gateway route) ----------

    @PostMapping("/internal/bookings")
    public Booking create(@RequestBody BookingRequest req) {
        Booking b = new Booking();
        b.setUsername(req.username());
        b.setSlotId(req.slotId());
        b.setSlotNumber(req.slotNumber());
        b.setStatus("ACTIVE");
        b.setStartTime(LocalDateTime.now());
        return repo.save(b);
    }

    // ---------- Public (through gateway). X-User / X-Role are set by the gateway from the JWT ----------

    @GetMapping("/api/bookings/my")
    public List<Booking> my(@RequestHeader("X-User") String username) {
        return repo.findByUsername(username);
    }

    @GetMapping("/api/bookings/all")
    public List<Booking> all(@RequestHeader("X-Role") String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
        return repo.findAll();
    }

    /** Driver leaves: booking completed, fee calculated, slot freed. */
    @PutMapping("/api/bookings/{id}/release")
    public Booking release(@PathVariable Long id, @RequestHeader("X-User") String username) {
        return finish(id, username, "COMPLETED");
    }

    /** Driver cancels: no fee, slot freed. */
    @PutMapping("/api/bookings/{id}/cancel")
    public Booking cancel(@PathVariable Long id, @RequestHeader("X-User") String username) {
        return finish(id, username, "CANCELLED");
    }

    private Booking finish(Long id, String username, String newStatus) {
        Booking b = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!b.getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        }
        if (!"ACTIVE".equals(b.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking already closed");
        }

        b.setEndTime(LocalDateTime.now());
        if ("COMPLETED".equals(newStatus)) {
            long minutes = Duration.between(b.getStartTime(), b.getEndTime()).toMinutes();
            long hours = Math.max(1, (long) Math.ceil(minutes / 60.0));   // minimum 1 hour charged
            b.setFee(hours * ratePerHour);
        } else {
            b.setFee(0);
        }
        b.setStatus(newStatus);

        parkingClient.release(b.getSlotId());   // slot becomes FREE again
        return repo.save(b);
    }
}
