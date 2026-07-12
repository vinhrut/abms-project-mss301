package com.abms.vehicle.service;

import com.abms.vehicle.dto.VehicleRequest;
import java.util.List;
import com.abms.vehicle.dto.VehicleResponse;
import java.util.UUID;

public interface VehicleService {

    VehicleResponse registerVehicle(VehicleRequest request);

    VehicleResponse updateVehicle(UUID vehicleId, VehicleRequest request);


    VehicleResponse approveVehicle(UUID vehicleId);

    VehicleResponse rejectVehicle(UUID vehicleId);

    List<VehicleResponse> getVehiclesByApartmentId(UUID apartmentId);

    List<VehicleResponse> getVehiclesByOwnerId(UUID ownerId);

    List<VehicleResponse> getAllVehicles();
}