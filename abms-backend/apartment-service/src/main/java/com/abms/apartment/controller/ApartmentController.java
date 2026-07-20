package com.abms.apartment.controller;

import com.abms.apartment.dto.ApartmentResidentResponse;
import com.abms.apartment.dto.ApartmentResponse;
import com.abms.apartment.dto.ResidentRegistrationRequest;
import com.abms.apartment.repository.ApartmentResidentRepository;
import com.abms.apartment.security.BuildingAccessService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/apartments")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final BuildingAccessService buildingAccessService;
    private final ApartmentResidentRepository apartmentResidentRepository;

    @GetMapping
    public ResponseEntity<List<ApartmentResponse>> getApartments(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "buildingId", required = false) UUID buildingId) {
        if (buildingId != null) {
            buildingAccessService.ensureCanViewApartments(authorizationHeader, buildingId);
            return ResponseEntity.ok(apartmentService.getApartmentsByBuildingId(buildingId));
        }
        buildingAccessService.ensureCanViewAllApartments(authorizationHeader);
        return ResponseEntity.ok(apartmentService.getAllApartments());
    }

    @GetMapping("/my")
    public ResponseEntity<List<ApartmentResponse>> getMyApartments(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(apartmentService.getMyApartments(userId));
    }

    @GetMapping("/{apartmentId}")
    public ResponseEntity<ApartmentResponse> getApartmentById(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable("apartmentId") UUID apartmentId) {
        ApartmentResponse apartment = apartmentService.getApartmentById(apartmentId);
        boolean activeResident = userId != null
                && apartmentResidentRepository.existsByApartmentIdAndUserIdAndStatus(apartmentId, userId, "ACTIVE");
        buildingAccessService.ensureCanViewApartmentDetail(authorizationHeader, apartmentId, apartment.getBuildingId(), activeResident);
        return ResponseEntity.ok(apartment);
    }

    @GetMapping("/{apartmentId}/residents")
    public ResponseEntity<List<ApartmentResidentResponse>> getResidentsByApartmentId(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("apartmentId") UUID apartmentId) {
        ApartmentResponse apartment = apartmentService.getApartmentById(apartmentId);
        buildingAccessService.ensureCanViewApartments(authorizationHeader, apartment.getBuildingId());
        return ResponseEntity.ok(apartmentService.getResidentsByApartmentId(authorizationHeader, apartmentId));
    }

    @PostMapping("/{apartmentId}/residents/{userId}/contracts/renew")
    public ResponseEntity<ApartmentResidentResponse> renewResidentContract(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("apartmentId") UUID apartmentId,
            @PathVariable("userId") UUID userId) {
        ApartmentResponse apartment = apartmentService.getApartmentById(apartmentId);
        buildingAccessService.ensureCanViewApartments(authorizationHeader, apartment.getBuildingId());
        return ResponseEntity.ok(apartmentService.renewResidentContract(authorizationHeader, apartmentId, userId));
    }

    @PostMapping("/{apartmentId}/residents/{userId}/remove")
    public ResponseEntity<ApartmentResidentResponse> removeResidentFromApartment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("apartmentId") UUID apartmentId,
            @PathVariable("userId") UUID userId) {
        ApartmentResponse apartment = apartmentService.getApartmentById(apartmentId);
        buildingAccessService.ensureCanViewApartments(authorizationHeader, apartment.getBuildingId());
        return ResponseEntity.ok(apartmentService.removeResidentFromApartment(authorizationHeader, apartmentId, userId));
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
