package com.abms.vehicle.controller;

import com.abms.vehicle.dto.VehicleRequest;
import com.abms.vehicle.dto.VehicleResponse;
import com.abms.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping("/")
    public ResponseEntity<VehicleResponse> registerVehicle(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.registerVehicle(request));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable("vehicleId") UUID vehicleId,
            @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(vehicleId, request));
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByApartment(@PathVariable("apartmentId") UUID apartmentId) {
        return ResponseEntity.ok(vehicleService.getVehiclesByApartmentId(apartmentId));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByOwner(@PathVariable("ownerId") UUID ownerId) {
        return ResponseEntity.ok(vehicleService.getVehiclesByOwnerId(ownerId));
    }

    @PostMapping("/{vehicleId}/approve")
    public ResponseEntity<VehicleResponse> approveVehicle(@PathVariable("vehicleId") UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.approveVehicle(vehicleId));
    }

    @PostMapping("/{vehicleId}/reject")
    public ResponseEntity<VehicleResponse> rejectVehicle(@PathVariable("vehicleId") UUID vehicleId) {
        return ResponseEntity.ok(vehicleService.rejectVehicle(vehicleId));
    }
}