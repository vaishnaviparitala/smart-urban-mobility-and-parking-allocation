package com.smartcity.auth;

public record RegisterRequest(String username, String password, String role) {
}
