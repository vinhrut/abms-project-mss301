package com.abms.auth.controller;

import com.abms.auth.dto.CreateManagerRequest;
import com.abms.auth.dto.CreateUserRequest;
import com.abms.auth.dto.UserResponse;
import com.abms.auth.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping("/managers")
    public ResponseEntity<UserResponse> createManager(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateManagerRequest request) {
        return ResponseEntity.ok(userManagementService.createManager(authorizationHeader, request));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userManagementService.createUser(authorizationHeader, request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(@RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(userManagementService.getUsers(authorizationHeader));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(userManagementService.getUserById(authorizationHeader, userId));
    }

    @PostMapping("/{userId}/lock")
    public ResponseEntity<UserResponse> lockUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(userManagementService.lockUser(authorizationHeader, userId));
    }

    @PostMapping("/{userId}/unlock")
    public ResponseEntity<UserResponse> unlockUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(userManagementService.unlockUser(authorizationHeader, userId));
    }
}