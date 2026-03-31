package com.br.elohostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.elohostel.model.BookingSync;

public interface BookingSyncRepository extends JpaRepository<BookingSync, Long> {
    Optional<BookingSync> findByPropertyId(String propertyId);
    List<BookingSync> findByIsActiveTrue();
}