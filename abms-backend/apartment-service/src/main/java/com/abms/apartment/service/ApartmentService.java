package com.abms.apartment.service;

import com.abms.apartment.dto.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ApartmentService {

    List<BuildingResponse> getAllBuildings();

    BuildingResponse getBuildingById(UUID buildingId);

    BuildingResponse createBuilding(BuildingRequest request);

    BuildingResponse updateBuilding(UUID buildingId, BuildingRequest request);

    void deleteBuilding(UUID buildingId);

    ApartmentResponse getApartmentById(UUID apartmentId);

    List<ApartmentResponse> getAllApartments();

    List<ApartmentResponse> getApartmentsByBuildingId(UUID buildingId);

    ApartmentResidentResponse createResidentRegistration(ResidentRegistrationRequest request);

    List<ApartmentResidentResponse> getPendingResidentRegistrations();

    ApartmentResidentResponse approveResidentRegistration(UUID userId);

    ApartmentResidentResponse rejectResidentRegistration(UUID userId);

    ApartmentResidentResponse getActiveResidenceByUserId(UUID userId);

    List<ApartmentResponse> getMyApartments(UUID userId);

    List<ApartmentResidentResponse> getResidentsByApartmentId(String authorizationHeader, UUID apartmentId);

    List<ApartmentResidentResponse> getResidentsByBuildingId(String authorizationHeader, UUID buildingId);

    @Transactional
    ApartmentResidentResponse renewResidentContract(String authorizationHeader, UUID apartmentId, UUID userId);

    @Transactional
    ApartmentResidentResponse removeResidentFromApartment(String authorizationHeader, UUID apartmentId, UUID userId);

    // --- contracts listing and management ---
    List<com.abms.apartment.dto.ContractResponse> listContracts(String authorizationHeader, UUID buildingId);

    com.abms.apartment.dto.ContractResponse getContractById(String authorizationHeader, UUID contractId);

    @Transactional
    com.abms.apartment.dto.ContractResponse renewContract(String authorizationHeader, UUID contractId, com.abms.apartment.dto.RenewContractRequest request);
}