package com.smartcity.allocation;

public record BookingDto(Long id, String username, Long slotId, String slotNumber, String status) {
}
