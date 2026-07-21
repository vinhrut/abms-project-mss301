package com.abms.apartment.controller;

import com.abms.apartment.dto.ApartmentResidentResponse;
import com.abms.apartment.dto.BuildingRequest;
import com.abms.apartment.dto.BuildingResponse;
import com.abms.apartment.security.BuildingAccessService;
import com.abms.apartment.service.ApartmentService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final ApartmentService apartmentService;
    private final BuildingAccessService buildingAccessService;

    @GetMapping
    public ResponseEntity<List<BuildingResponse>> getBuildings() {
        return ResponseEntity.ok(apartmentService.getAllBuildings());
    }

    @GetMapping("/{buildingId}")
    public ResponseEntity<BuildingResponse> getBuildingById(@PathVariable("buildingId") UUID buildingId) {
        return ResponseEntity.ok(apartmentService.getBuildingById(buildingId));
    }

    @PostMapping
    public ResponseEntity<BuildingResponse> createBuilding(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody BuildingRequest request) {
        buildingAccessService.ensureAdmin(authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(apartmentService.createBuilding(request));
    }

    @PutMapping("/{buildingId}")
    public ResponseEntity<BuildingResponse> updateBuilding(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("buildingId") UUID buildingId,
            @Valid @RequestBody BuildingRequest request) {
        buildingAccessService.ensureCanManageBuilding(authorizationHeader, buildingId);
        return ResponseEntity.ok(apartmentService.updateBuilding(buildingId, request));
    }

    @DeleteMapping("/{buildingId}")
    public ResponseEntity<Void> deleteBuilding(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("buildingId") UUID buildingId) {
        buildingAccessService.ensureAdmin(authorizationHeader);
        apartmentService.deleteBuilding(buildingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{buildingId}/apartments")
    public ResponseEntity<List<com.abms.apartment.dto.ApartmentResponse>> getApartmentsByBuildingId(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("buildingId") UUID buildingId) {
        buildingAccessService.ensureCanViewApartments(authorizationHeader, buildingId);
        return ResponseEntity.ok(apartmentService.getApartmentsByBuildingId(buildingId));
    }

    @GetMapping("/{buildingId}/residents")
    public ResponseEntity<List<ApartmentResidentResponse>> getResidentsByBuildingId(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("buildingId") UUID buildingId) {
        buildingAccessService.ensureCanViewApartments(authorizationHeader, buildingId);
        return ResponseEntity.ok(apartmentService.getResidentsByBuildingId(authorizationHeader, buildingId));
    }
}
