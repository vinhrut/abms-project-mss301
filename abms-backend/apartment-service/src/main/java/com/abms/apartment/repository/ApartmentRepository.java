package com.abms.apartment.repository;

import com.abms.apartment.entity.Apartment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentRepository extends JpaRepository<Apartment, UUID> {

    java.util.List<Apartment> findAllByOrderByRoomNumberAsc();
}