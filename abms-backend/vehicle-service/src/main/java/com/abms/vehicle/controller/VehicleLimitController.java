package com.abms.vehicle.controller;

import com.abms.vehicle.dto.VehicleLimitRequest;
import com.abms.vehicle.dto.VehicleLimitResponse;
import com.abms.vehicle.security.CurrentUser;
import com.abms.vehicle.security.CurrentUserResolver;
import com.abms.vehicle.service.VehicleLimitService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles/limits")
@RequiredArgsConstructor
public class VehicleLimitController {

    private final VehicleLimitService vehicleLimitService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<List<VehicleLimitResponse>> getLimits(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @RequestParam(value = "apartmentId", required = false) UUID apartmentId) {
        return ResponseEntity.ok(vehicleLimitService.getLimits(resolve(userId, role, buildingId), apartmentId));
    }

    @GetMapping("/{limitId}")
    public ResponseEntity<VehicleLimitResponse> getLimitById(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @PathVariable("limitId") UUID limitId) {
        return ResponseEntity.ok(vehicleLimitService.getLimitById(resolve(userId, role, buildingId), limitId));
    }

    @PostMapping
    public ResponseEntity<VehicleLimitResponse> createLimit(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @Valid @RequestBody VehicleLimitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleLimitService.createLimit(resolve(userId, role, buildingId), request));
    }

    @PutMapping("/{limitId}")
    public ResponseEntity<VehicleLimitResponse> updateLimit(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @PathVariable("limitId") UUID limitId,
            @Valid @RequestBody VehicleLimitRequest request) {
        return ResponseEntity.ok(vehicleLimitService.updateLimit(resolve(userId, role, buildingId), limitId, request));
    }

    @DeleteMapping("/{limitId}")
    public ResponseEntity<Void> deleteLimit(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @PathVariable("limitId") UUID limitId) {
        vehicleLimitService.deleteLimit(resolve(userId, role, buildingId), limitId);
        return ResponseEntity.noContent().build();
    }

    private CurrentUser resolve(String userId, String role, String buildingId) {
        return currentUserResolver.resolve(userId, role, buildingId);
    }
}