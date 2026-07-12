 package com.abms.apartment.config;

import com.abms.apartment.entity.Apartment;
import com.abms.apartment.entity.ApartmentResident;
import com.abms.apartment.repository.ApartmentRepository;
import com.abms.apartment.repository.ApartmentResidentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ApartmentRepository apartmentRepository;
    private final ApartmentResidentRepository apartmentResidentRepository;

    @Override
    public void run(String... args) {
        if (apartmentRepository.count() == 0) {
            apartmentRepository.save(Apartment.builder()
                    .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                    .buildingId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                    .roomNumber("A-101")
                    .floor(1)
                    .area(new BigDecimal("75.50"))
                    .status("OCCUPIED")
                    .build());

            apartmentRepository.save(Apartment.builder()
                    .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab"))
                    .buildingId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                    .roomNumber("A-102")
                    .floor(1)
                    .area(new BigDecimal("68.00"))
                    .status("OCCUPIED")
                    .build());
        }

        if (apartmentResidentRepository.count() > 0) {
            return;
        }

        apartmentResidentRepository.save(ApartmentResident.builder()
                .residentId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1"))
                .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .relationship("OWNER")
                .residenceType("PERMANENT")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().minusDays(10))
                .approvedAt(LocalDateTime.now().minusDays(9))
                .build());

        apartmentResidentRepository.save(ApartmentResident.builder()
                .residentId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2"))
                .apartmentId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab"))
                .userId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .relationship("TENANT")
                .residenceType("TEMPORARY")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().minusDays(8))
                .approvedAt(LocalDateTime.now().minusDays(7))
                .build());
    }
}