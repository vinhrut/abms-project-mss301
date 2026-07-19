package com.abms.finance.controller;

import com.abms.finance.dto.ServiceRequest;
import com.abms.finance.dto.ServiceResponse;
import com.abms.finance.service.BillingServiceCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceController {

    private final BillingServiceCatalogService billingServiceCatalogService;

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getAllServices() {
        return ResponseEntity.ok(billingServiceCatalogService.getAllServices());
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable("serviceId") Integer serviceId) {
        return ResponseEntity.ok(billingServiceCatalogService.getServiceById(serviceId));
    }

    @PostMapping("/")
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingServiceCatalogService.createService(request));
    }

    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable("serviceId") Integer serviceId,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(billingServiceCatalogService.updateService(serviceId, request));
    }
}
