package com.abms.vehicle.client;

import com.abms.vehicle.dto.ApartmentResidentResponse;
import com.abms.vehicle.dto.ApartmentResponse;
import com.abms.vehicle.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ApartmentClient {

    private final RestTemplate restTemplate;

    @Value("${services.apartment-service.url:http://localhost:8082}")
    private String apartmentServiceUrl;

    public ApartmentResponse getApartmentById(UUID apartmentId) {
        try {
            ResponseEntity<ApartmentResponse> response = restTemplate.exchange(
                    apartmentServiceUrl + "/internal/apartments/" + apartmentId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Apartment not found: " + apartmentId);
            }
            throw ex;
        }
    }

    public ApartmentResidentResponse getActiveResidenceByUserId(UUID userId) {
        try {
            ResponseEntity<ApartmentResidentResponse> response = restTemplate.exchange(
                    apartmentServiceUrl + "/internal/apartments/residents/user/" + userId + "/active",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Active residence not found for user: " + userId);
            }
            throw ex;
        }
    }

    public List<ApartmentResponse> getApartmentsByBuildingId(UUID buildingId) {
        try {
            ResponseEntity<List<ApartmentResponse>> response = restTemplate.exchange(
                    apartmentServiceUrl + "/internal/buildings/" + buildingId + "/apartments",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Building not found: " + buildingId);
            }
            throw ex;
        }
    }
}