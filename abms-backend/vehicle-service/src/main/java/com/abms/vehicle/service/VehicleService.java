package com.abms.vehicle.service;

import com.abms.vehicle.dto.VehicleRequest;
import java.util.List;
import com.abms.vehicle.dto.VehicleResponse;
import com.abms.vehicle.security.CurrentUser;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehicleService {

    VehicleResponse registerVehicle(CurrentUser actor, VehicleRequest request);

    VehicleResponse updateVehicle(CurrentUser actor, UUID vehicleId, VehicleRequest request);

    VehicleResponse getVehicleById(CurrentUser actor, UUID vehicleId);

    VehicleResponse approveVehicle(CurrentUser actor, UUID vehicleId);

    VehicleResponse rejectVehicle(CurrentUser actor, UUID vehicleId);

    VehicleResponse cancelVehicle(CurrentUser actor, UUID vehicleId);

    List<VehicleResponse> getMyVehicles(CurrentUser actor);

    Page<VehicleResponse> searchVehicles(CurrentUser actor, String status, String type, String licensePlate, UUID apartmentId, UUID ownerId, Pageable pageable);
}