package com.smartcity.allocation;

public record SlotDto(Long id, String lotName, String slotNumber, String vehicleType,
                      String status, int distanceFromEntrance) {
}
