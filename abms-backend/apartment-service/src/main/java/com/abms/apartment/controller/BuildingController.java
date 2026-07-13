package com.abms.apartment.controller;

import com.abms.apartment.dto.BuildingResponse;
import com.abms.apartment.service.ApartmentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final ApartmentService apartmentService;

    @GetMapping
    public ResponseEntity<List<BuildingResponse>> getBuildings() {
        return ResponseEntity.ok(apartmentService.getAllBuildings());
    }

    @GetMapping("/{buildingId}")
    public ResponseEntity<BuildingResponse> getBuildingById(@PathVariable("buildingId") UUID buildingId) {
        return ResponseEntity.ok(apartmentService.getBuildingById(buildingId));
    }

    @GetMapping("/{buildingId}/apartments")
    public ResponseEntity<List<com.abms.apartment.dto.ApartmentResponse>> getApartmentsByBuildingId(@PathVariable("buildingId") UUID buildingId) {
        return ResponseEntity.ok(apartmentService.getApartmentsByBuildingId(buildingId));
    }
}