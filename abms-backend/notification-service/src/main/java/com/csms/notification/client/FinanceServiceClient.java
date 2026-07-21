package com.csms.notification.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.finance-service.url:http://localhost:8086}")
    private String financeServiceUrl;

    public List<FinanceInvoiceResponse> getInvoicesByBillingMonth(YearMonth period) {
        LocalDate billingMonth = period.atDay(1);
        String url = UriComponentsBuilder
                .fromHttpUrl(financeServiceUrl + "/api/v1/invoices")
                .queryParam("billingMonth", billingMonth.toString())
                .toUriString();
        try {
            ResponseEntity<List<FinanceInvoiceResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (Exception exception) {
            log.warn("Failed to load invoices from finance-service for {}: {}", period, exception.getMessage());
            return Collections.emptyList();
        }
    }
}
