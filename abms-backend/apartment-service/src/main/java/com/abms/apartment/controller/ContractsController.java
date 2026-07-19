package com.abms.apartment.controller;

import com.abms.apartment.dto.ContractResponse;
import com.abms.apartment.dto.RenewContractRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractsController {

    private final ApartmentService apartmentService;
    private final BuildingAccessService buildingAccessService;

    @GetMapping
    public ResponseEntity<List<ContractResponse>> listContracts(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "buildingId", required = false) UUID buildingId) {
        if (buildingId != null) {
            buildingAccessService.ensureCanViewApartments(authorizationHeader, buildingId);
        } else {
            buildingAccessService.ensureCanViewAllApartments(authorizationHeader);
        }
        return ResponseEntity.ok(apartmentService.listContracts(authorizationHeader, buildingId));
    }

    @GetMapping("/{contractId}")
    public ResponseEntity<ContractResponse> getContractById(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("contractId") UUID contractId) {
        ContractResponse resp = apartmentService.getContractById(authorizationHeader, contractId);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        if (resp.getApartmentId() != null) {
            var apartment = apartmentService.getApartmentById(resp.getApartmentId());
            buildingAccessService.ensureCanViewApartments(authorizationHeader, apartment.getBuildingId());
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{contractId}/renew")
    public ResponseEntity<ContractResponse> renewContract(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("contractId") UUID contractId,
            @Valid @RequestBody RenewContractRequest request) {
        var updated = apartmentService.renewContract(authorizationHeader, contractId, request);
        return ResponseEntity.ok(updated);
    }
}
