package com.smartcity.allocation;

public record BookingRequest(String username, Long slotId, String slotNumber) {
}
