package com.smartcity.booking;

public record BookingRequest(String username, Long slotId, String slotNumber) {
}
