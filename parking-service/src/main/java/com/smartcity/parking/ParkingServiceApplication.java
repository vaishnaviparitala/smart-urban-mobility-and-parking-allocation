package com.smartcity.parking;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ParkingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkingServiceApplication.class, args);
    }

    /** Loads sample slots every time the in-memory database starts. */
    @Bean
    CommandLineRunner seed(ParkingSlotRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(slot("Central Lot", "A1", "CAR", 1));
                repo.save(slot("Central Lot", "A2", "CAR", 2));
                repo.save(slot("Central Lot", "A3", "CAR", 3));
                repo.save(slot("Central Lot", "B1", "BIKE", 1));
                repo.save(slot("Central Lot", "B2", "BIKE", 2));
            }
        };
    }

    private ParkingSlot slot(String lot, String number, String type, int distance) {
        ParkingSlot s = new ParkingSlot();
        s.setLotName(lot);
        s.setSlotNumber(number);
        s.setVehicleType(type);
        s.setStatus("FREE");
        s.setDistanceFromEntrance(distance);
        return s;
    }
}
