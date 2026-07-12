package com.abms.apartment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "apartment_residents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentResident {

    @Id
    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "apartment_id", nullable = false)
    private UUID apartmentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "relationship", nullable = false)
    private String relationship;

    @Column(name = "residence_type", nullable = false)
    private String residenceType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
}