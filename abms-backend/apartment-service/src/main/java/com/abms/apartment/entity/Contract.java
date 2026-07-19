package com.abms.apartment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "apartment_id", nullable = false)
    private UUID apartmentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "contract_type")
    private String contractType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "deposit")
    private BigDecimal deposit;

    @Column(name = "status")
    private String status;
}
