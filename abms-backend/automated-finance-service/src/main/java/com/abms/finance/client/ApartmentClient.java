package com.abms.finance.client;

import com.abms.finance.dto.ApartmentResponse;
import com.abms.finance.exception.ResourceNotFoundException;
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
}
