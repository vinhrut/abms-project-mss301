package com.abms.apartment.service;

import com.abms.apartment.dto.BuildingResponse;
import com.abms.apartment.dto.ApartmentResidentResponse;
import com.abms.apartment.dto.ApartmentResponse;
import com.abms.apartment.dto.ResidentRegistrationRequest;
import java.util.List;
import java.util.UUID;

public interface ApartmentService {

    List<BuildingResponse> getAllBuildings();

    BuildingResponse getBuildingById(UUID buildingId);

    ApartmentResponse getApartmentById(UUID apartmentId);

    List<ApartmentResponse> getAllApartments();

    List<ApartmentResponse> getApartmentsByBuildingId(UUID buildingId);

    List<ApartmentResidentResponse> getResidentsByApartmentId(String authorizationHeader, UUID apartmentId);

    List<ApartmentResidentResponse> getResidentsByBuildingId(String authorizationHeader, UUID buildingId);

    ApartmentResidentResponse renewResidentContract(String authorizationHeader, UUID apartmentId, UUID userId);

    ApartmentResidentResponse removeResidentFromApartment(String authorizationHeader, UUID apartmentId, UUID userId);

    ApartmentResidentResponse createResidentRegistration(ResidentRegistrationRequest request);

    List<ApartmentResidentResponse> getPendingResidentRegistrations();

    ApartmentResidentResponse approveResidentRegistration(UUID userId);

    ApartmentResidentResponse rejectResidentRegistration(UUID userId);

    ApartmentResidentResponse getActiveResidenceByUserId(UUID userId);
}