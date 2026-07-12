package com.abms.apartment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "apartments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Apartment {

    @Id
    @Column(name = "apartment_id", nullable = false)
    private UUID apartmentId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Column(name = "floor", nullable = false)
    private int floor;

    @Column(name = "area", nullable = false)
    private BigDecimal area;

    @Column(name = "status", nullable = false)
    private String status;
}