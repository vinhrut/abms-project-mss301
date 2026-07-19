package com.abms.maintenance.client;

import com.abms.maintenance.dto.ApartmentResidentResponse;
import com.abms.maintenance.dto.ApartmentResponse;
import com.abms.maintenance.exception.ResourceNotFoundException;
import java.util.Optional;
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
                    apartmentServiceUrl + "/api/v1/apartments/" + apartmentId,
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

    public Optional<ApartmentResidentResponse> findActiveResidenceByUserId(UUID userId) {
        try {
            ResponseEntity<ApartmentResidentResponse> response = restTemplate.exchange(
                    apartmentServiceUrl + "/api/v1/apartments/residents/user/" + userId + "/active",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    public ApartmentResidentResponse getActiveResidenceByUserId(UUID userId) {
        return findActiveResidenceByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Active residence not found for user: " + userId));
    }
}
