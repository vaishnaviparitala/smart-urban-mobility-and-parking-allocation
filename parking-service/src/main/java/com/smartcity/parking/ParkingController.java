package com.smartcity.parking;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ParkingController {

    private static final Set<String> VEHICLE_TYPES = Set.of("CAR", "BIKE");

    private final ParkingSlotRepository repo;

    public ParkingController(ParkingSlotRepository repo) {
        this.repo = repo;
    }

    // ---------- Public endpoints (reached through the gateway) ----------

    @GetMapping("/api/parking/slots")
    public List<ParkingSlot> slots(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) String vehicleType) {
        String st = status == null ? null : status.toUpperCase();
        String vt = vehicleType == null ? null : vehicleType.toUpperCase();

        if (st != null && vt != null) {
            return repo.findByStatusAndVehicleTypeOrderByDistanceFromEntranceAsc(st, vt);
        } else if (st != null) {
            return repo.findByStatus(st);
        } else if (vt != null) {
            return repo.findByVehicleType(vt);
        }
        return repo.findAll();
    }

    @GetMapping("/api/parking/summary")
    public Map<String, Long> summary() {
        return Map.of("total", repo.count(),
                      "free", repo.countByStatus("FREE"),
                      "reserved", repo.countByStatus("RESERVED"));
    }

    // ---------- ADMIN only (role rule is enforced in the gateway) ----------

    @PostMapping("/api/parking/slots")
    public ParkingSlot add(@RequestBody ParkingSlot slot) {
        if (slot.getSlotNumber() == null || slot.getSlotNumber().isBlank()
                || slot.getVehicleType() == null
                || !VEHICLE_TYPES.contains(slot.getVehicleType().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "slotNumber is required and vehicleType must be CAR or BIKE");
        }
        slot.setId(null);
        slot.setVehicleType(slot.getVehicleType().toUpperCase());
        slot.setStatus("FREE");
        return repo.save(slot);
    }

    @DeleteMapping("/api/parking/slots/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        ParkingSlot s = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (!"FREE".equals(s.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot is currently reserved");
        }
        repo.delete(s);
        return Map.of("message", "Slot deleted");
    }

    // ---------- Internal endpoints (no gateway route, only other services call these) ----------

    @PutMapping("/internal/parking/slots/{id}/reserve")
    public boolean reserve(@PathVariable Long id) {
        return repo.changeStatus(id, "FREE", "RESERVED") == 1;
    }

    @PutMapping("/internal/parking/slots/{id}/release")
    public boolean release(@PathVariable Long id) {
        return repo.changeStatus(id, "RESERVED", "FREE") == 1;
    }
}
