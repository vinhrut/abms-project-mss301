package com.abms.apartment.controller;

import com.abms.apartment.dto.ApartmentResidentResponse;
import com.abms.apartment.dto.ApartmentResponse;
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
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalApartmentController {

    private final ApartmentService apartmentService;

    @GetMapping("/apartments/{apartmentId}")
    public ResponseEntity<ApartmentResponse> getApartmentById(@PathVariable("apartmentId") UUID apartmentId) {
        return ResponseEntity.ok(apartmentService.getApartmentById(apartmentId));
    }

    @GetMapping("/buildings/{buildingId}/apartments")
    public ResponseEntity<List<ApartmentResponse>> getApartmentsByBuildingId(@PathVariable("buildingId") UUID buildingId) {
        return ResponseEntity.ok(apartmentService.getApartmentsByBuildingId(buildingId));
    }

    @GetMapping("/apartments/residents/user/{userId}/active")
    public ResponseEntity<ApartmentResidentResponse> getActiveResidenceByUserId(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(apartmentService.getActiveResidenceByUserId(userId));
    }

    @GetMapping("/apartments/{apartmentId}/residents/{userId}/active")
    public ResponseEntity<Void> checkActiveResidence(
            @PathVariable("apartmentId") UUID apartmentId,
            @PathVariable("userId") UUID userId) {
        boolean exists = apartmentService.getMyApartments(userId)
                .stream()
                .anyMatch(apartment -> apartmentId.equals(apartment.getApartmentId()));
        return exists ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/apartments/{apartmentId}/residents")
    public ResponseEntity<List<ApartmentResidentResponse>> getResidentsByApartmentId(
            @PathVariable("apartmentId") UUID apartmentId) {
        return ResponseEntity.ok(apartmentService.getResidentsByApartmentId(null, apartmentId));
    }
}