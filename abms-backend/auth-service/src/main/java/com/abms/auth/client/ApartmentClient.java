package com.abms.auth.client;

import com.abms.auth.dto.ApartmentResidentResponse;
import com.abms.auth.dto.ApartmentResponse;
import com.abms.auth.dto.BuildingResponse;
import com.abms.auth.dto.ResidentRegistrationRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ApartmentClient {

    private final RestTemplate restTemplate;

    @Value("${services.apartment-service.url:http://localhost:8082}")
    private String apartmentServiceUrl;

    public void createResidentRegistration(ResidentRegistrationRequest request) {
        restTemplate.exchange(
                apartmentServiceUrl + "/api/v1/apartments/residents/registrations",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApartmentResidentResponse>() {
                });
    }

    public List<ApartmentResidentResponse> getPendingResidentRegistrations() {
        return restTemplate.exchange(
                        apartmentServiceUrl + "/api/v1/apartments/residents/pending",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<ApartmentResidentResponse>>() {
                        })
                .getBody();
    }

    public void approveResidentRegistration(UUID userId) {
        restTemplate.exchange(
                apartmentServiceUrl + "/api/v1/apartments/residents/" + userId + "/approve",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ApartmentResidentResponse>() {
                });
    }

    public void rejectResidentRegistration(UUID userId) {
        restTemplate.exchange(
                apartmentServiceUrl + "/api/v1/apartments/residents/" + userId + "/reject",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<ApartmentResidentResponse>() {
                });
    }

    public ApartmentResponse getApartmentById(UUID apartmentId) {
        return restTemplate.exchange(
                        apartmentServiceUrl + "/api/v1/apartments/" + apartmentId,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<ApartmentResponse>() {
                        })
                .getBody();
    }

    public BuildingResponse getBuildingById(UUID buildingId) {
        return restTemplate.exchange(
                        apartmentServiceUrl + "/api/v1/buildings/" + buildingId,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<BuildingResponse>() {
                        })
                .getBody();
    }
}