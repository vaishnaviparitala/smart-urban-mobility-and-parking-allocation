package com.smartcity.auth;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository repo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository repo, JwtUtil jwtUtil) {
        this.repo = repo;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest req) {
        if (req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username is required and password must be at least 6 characters"));
        }
        if (repo.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already exists"));
        }

        User u = new User();
        u.setUsername(req.username());
        u.setPassword(encoder.encode(req.password()));              // hashed with BCrypt
        // DEMO ONLY: lets you create an ADMIN from Postman. A real system would create admins separately.
        u.setRole("ADMIN".equalsIgnoreCase(req.role()) ? "ADMIN" : "USER");
        repo.save(u);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req) {
        Optional<User> user = repo.findByUsername(req.username() == null ? "" : req.username());

        if (user.isEmpty() || req.password() == null
                || !encoder.matches(req.password(), user.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generate(user.get().getUsername(), user.get().getRole());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.get().getUsername(),
                "role", user.get().getRole(),
                "expiresInMs", jwtUtil.getExpirationMs()));
    }
}
