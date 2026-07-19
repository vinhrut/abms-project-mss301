package com.abms.vehicle.controller;

import com.abms.vehicle.dto.VehicleRequest;
import com.abms.vehicle.dto.VehicleResponse;
import com.abms.vehicle.dto.VehicleStatusRequest;
import com.abms.vehicle.security.CurrentUser;
import com.abms.vehicle.security.CurrentUserResolver;
import com.abms.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping({"/", "/register"})
    public ResponseEntity<VehicleResponse> registerVehicle(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.registerVehicle(resolve(userId, role, buildingId), request));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @PathVariable("vehicleId") UUID vehicleId,
            @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(resolve(userId, role, buildingId), vehicleId, request));
    }

    @GetMapping
    public ResponseEntity<Page<VehicleResponse>> searchVehicles(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "licensePlate", required = false) String licensePlate,
            @RequestParam(value = "apartmentId", required = false) UUID apartmentId,
            @RequestParam(value = "ownerId", required = false) UUID ownerId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("status").ascending().and(Sort.by("licensePlate").ascending()));
        return ResponseEntity.ok(vehicleService.searchVehicles(resolve(userId, role, buildingId), status, type, licensePlate, apartmentId, ownerId, pageable));
    }

    @GetMapping("/my")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId) {
        return ResponseEntity.ok(vehicleService.getMyVehicles(resolve(userId, role, buildingId)));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> getVehicleById(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @PathVariable("vehicleId") UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.getVehicleById(resolve(userId, role, buildingId), vehicleId));
    }

    @PostMapping("/{vehicleId}/approve")
    public ResponseEntity<VehicleResponse> approveVehicle(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @PathVariable("vehicleId") UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.approveVehicle(resolve(userId, role, buildingId), vehicleId));
    }

    @PostMapping("/{vehicleId}/reject")
    public ResponseEntity<VehicleResponse> rejectVehicle(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @PathVariable("vehicleId") UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.rejectVehicle(resolve(userId, role, buildingId), vehicleId));
    }

    @PostMapping("/{vehicleId}/cancel")
    public ResponseEntity<VehicleResponse> cancelVehicle(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @PathVariable("vehicleId") UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.cancelVehicle(resolve(userId, role, buildingId), vehicleId));
    }

    @PutMapping("/{vehicleId}/status")
    public ResponseEntity<VehicleResponse> updateVehicleStatus(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) String buildingId,
            @PathVariable("vehicleId") UUID vehicleId,
            @Valid @RequestBody VehicleStatusRequest request) {
        CurrentUser actor = resolve(userId, role, buildingId);
        if ("APPROVED".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.ok(vehicleService.approveVehicle(actor, vehicleId));
        }
        if ("REJECTED".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.ok(vehicleService.rejectVehicle(actor, vehicleId));
        }
        throw new com.abms.vehicle.exception.InvalidVehicleStatusException("Status must be APPROVED or REJECTED");
    }

    private CurrentUser resolve(String userId, String role, String buildingId) {
        return currentUserResolver.resolve(userId, role, buildingId);
    }
}