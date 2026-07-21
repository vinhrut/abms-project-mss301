package com.csms.notification.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApartmentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.apartment-service.url:http://localhost:8082}")
    private String apartmentServiceUrl;

    public List<ApartmentResidentClientResponse> getResidentsByApartmentId(UUID apartmentId) {
        if (apartmentId == null) {
            return Collections.emptyList();
        }
        String url = apartmentServiceUrl + "/internal/apartments/" + apartmentId + "/residents";
        try {
            ResponseEntity<List<ApartmentResidentClientResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (Exception exception) {
            log.warn("Failed to load residents for apartment {} from apartment-service: {}",
                    apartmentId, exception.getMessage());
            return Collections.emptyList();
        }
    }
}
