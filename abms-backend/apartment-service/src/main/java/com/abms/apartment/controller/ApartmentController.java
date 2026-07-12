package com.abms.apartment.controller;

import com.abms.apartment.dto.ApartmentResidentResponse;
import com.abms.apartment.dto.ApartmentResponse;
import com.abms.apartment.dto.ResidentRegistrationRequest;
import com.abms.apartment.service.ApartmentService;
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
@RequestMapping("/api/v1/apartments")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;

    @GetMapping
    public ResponseEntity<List<ApartmentResponse>> getApartments() {
        return ResponseEntity.ok(apartmentService.getAllApartments());
    }

    @GetMapping("/{apartmentId}")
    public ResponseEntity<ApartmentResponse> getApartmentById(@PathVariable("apartmentId") UUID apartmentId) {
        return ResponseEntity.ok(apartmentService.getApartmentById(apartmentId));
    }

    @PostMapping("/residents/registrations")
    public ResponseEntity<ApartmentResidentResponse> createResidentRegistration(
            @Valid @RequestBody ResidentRegistrationRequest request) {
        return ResponseEntity.ok(apartmentService.createResidentRegistration(request));
    }

    @GetMapping("/residents/pending")
    public ResponseEntity<List<ApartmentResidentResponse>> getPendingResidentRegistrations() {
        return ResponseEntity.ok(apartmentService.getPendingResidentRegistrations());
    }

    @PostMapping("/residents/{userId}/approve")
    public ResponseEntity<ApartmentResidentResponse> approveResidentRegistration(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(apartmentService.approveResidentRegistration(userId));
    }

    @PostMapping("/residents/{userId}/reject")
    public ResponseEntity<ApartmentResidentResponse> rejectResidentRegistration(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(apartmentService.rejectResidentRegistration(userId));
    }

    @GetMapping("/residents/user/{userId}/active")
    public ResponseEntity<ApartmentResidentResponse> getActiveResidenceByUserId(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(apartmentService.getActiveResidenceByUserId(userId));
    }
}