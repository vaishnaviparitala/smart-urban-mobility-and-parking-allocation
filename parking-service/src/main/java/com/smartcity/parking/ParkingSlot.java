package com.smartcity.parking;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lotName;
    private String slotNumber;
    private String vehicleType;          // CAR or BIKE
    private String status;               // FREE or RESERVED
    private int distanceFromEntrance;    // smaller number = nearer to the entrance

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLotName() { return lotName; }
    public void setLotName(String lotName) { this.lotName = lotName; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getDistanceFromEntrance() { return distanceFromEntrance; }
    public void setDistanceFromEntrance(int distanceFromEntrance) { this.distanceFromEntrance = distanceFromEntrance; }
}
