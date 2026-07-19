package com.abms.finance.service;

import com.abms.finance.dto.ServiceRequest;
import com.abms.finance.dto.ServiceResponse;
import java.util.List;

public interface BillingServiceCatalogService {

    List<ServiceResponse> getAllServices();

    ServiceResponse getServiceById(Integer serviceId);

    ServiceResponse createService(ServiceRequest request);

    ServiceResponse updateService(Integer serviceId, ServiceRequest request);
}
