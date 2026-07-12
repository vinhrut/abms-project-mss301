package com.abms.auth.controller;

import com.abms.auth.dto.AuthResponse;
import com.abms.auth.dto.LoginRequest;
import com.abms.auth.dto.RegisterRequest;
import com.abms.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/residents/pending")
    public ResponseEntity<List<AuthResponse>> getPendingResidents() {
        return ResponseEntity.ok(authService.getPendingResidents());
    }

    @PostMapping("/residents/{userId}/approve")
    public ResponseEntity<AuthResponse> approveResident(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(authService.approveResident(userId));
    }

    @PostMapping("/residents/{userId}/reject")
    public ResponseEntity<AuthResponse> rejectResident(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(authService.rejectResident(userId));
    }
}