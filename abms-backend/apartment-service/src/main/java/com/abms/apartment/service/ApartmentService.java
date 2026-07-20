package com.abms.apartment.service;

import com.abms.apartment.dto.BuildingResponse;
import com.abms.apartment.dto.ApartmentResidentResponse;
import com.abms.apartment.dto.ApartmentResponse;
import com.abms.apartment.dto.ContractResponse;
import com.abms.apartment.dto.RenewContractRequest;
import com.abms.apartment.dto.ResidentRegistrationRequest;
import java.util.List;
import java.util.UUID;

public interface ApartmentService {

    List<BuildingResponse> getAllBuildings();

    BuildingResponse getBuildingById(UUID buildingId);

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

    ApartmentResidentResponse renewResidentContract(String authorizationHeader, UUID apartmentId, UUID userId);

    ApartmentResidentResponse removeResidentFromApartment(String authorizationHeader, UUID apartmentId, UUID userId);

    List<ContractResponse> listContracts(String authorizationHeader, UUID buildingId);

    ContractResponse getContractById(String authorizationHeader, UUID contractId);

    ContractResponse renewContract(String authorizationHeader, UUID contractId, RenewContractRequest request);
}