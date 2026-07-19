package com.abms.finance.service.impl;

import com.abms.finance.dto.ServiceRequest;
import com.abms.finance.dto.ServiceResponse;
import com.abms.finance.entity.ServiceItem;
import com.abms.finance.exception.ResourceNotFoundException;
import com.abms.finance.repository.ServiceItemRepository;
import com.abms.finance.service.BillingServiceCatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingServiceCatalogServiceImpl implements BillingServiceCatalogService {

    private final ServiceItemRepository serviceItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getAllServices() {
        return serviceItemRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Integer serviceId) {
        return toResponse(findService(serviceId));
    }

    @Override
    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        Integer nextId = serviceItemRepository.findAll().stream()
                .map(ServiceItem::getServiceId)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        ServiceItem service = ServiceItem.builder()
                .serviceId(nextId)
                .name(request.getName())
                .unitPrice(request.getUnitPrice())
                .unit(request.getUnit())
                .build();

        return toResponse(serviceItemRepository.save(service));
    }

    @Override
    @Transactional
    public ServiceResponse updateService(Integer serviceId, ServiceRequest request) {
        ServiceItem service = findService(serviceId);
        service.setName(request.getName());
        service.setUnitPrice(request.getUnitPrice());
        service.setUnit(request.getUnit());
        return toResponse(serviceItemRepository.save(service));
    }

    private ServiceItem findService(Integer serviceId) {
        return serviceItemRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
    }

    private ServiceResponse toResponse(ServiceItem service) {
        return ServiceResponse.builder()
                .serviceId(service.getServiceId())
                .name(service.getName())
                .unitPrice(service.getUnitPrice())
                .unit(service.getUnit())
                .build();
    }
}
