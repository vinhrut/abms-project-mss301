package com.abms.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle_limits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLimit {

    @Id
    @Column(name = "limit_id", nullable = false)
    private UUID limitId;

    @Column(name = "apartment_id", nullable = false)
    private UUID apartmentId;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;

    @Column(name = "max_quantity", nullable = false)
    private int maxQuantity;
}