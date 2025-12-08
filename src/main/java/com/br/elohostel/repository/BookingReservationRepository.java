package com.br.elohostel.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.elohostel.model.BookingReservation;

public interface BookingReservationRepository extends JpaRepository<BookingReservation, Long> {
    Optional<BookingReservation> findByBookingReservationId(String bookingReservationId);
}
