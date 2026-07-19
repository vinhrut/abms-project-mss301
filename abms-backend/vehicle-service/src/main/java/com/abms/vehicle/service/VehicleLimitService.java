package com.abms.vehicle.service;

import com.abms.vehicle.dto.VehicleLimitRequest;
import com.abms.vehicle.dto.VehicleLimitResponse;
import com.abms.vehicle.security.CurrentUser;
import java.util.List;
import java.util.UUID;

public interface VehicleLimitService {

    VehicleLimitResponse createLimit(CurrentUser actor, VehicleLimitRequest request);

    VehicleLimitResponse updateLimit(CurrentUser actor, UUID limitId, VehicleLimitRequest request);

    void deleteLimit(CurrentUser actor, UUID limitId);

    VehicleLimitResponse getLimitById(CurrentUser actor, UUID limitId);

    List<VehicleLimitResponse> getLimits(CurrentUser actor, UUID apartmentId);
}