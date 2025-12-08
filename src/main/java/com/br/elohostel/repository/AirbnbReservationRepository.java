package com.br.elohostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.AirbnbReservation;
import com.br.elohostel.model.AirbnbSync;

@Repository
public interface AirbnbReservationRepository  extends JpaRepository<AirbnbReservation, Long> {

    Optional<AirbnbReservation> findByAirbnbReservationId(String airbnbReservationId);

    //  Optional<AirbnbReservation> findByAirbnbReservationId(String airbnbReservationId);
    List<AirbnbReservation> findByAirbnbSyncAndIsProcessedFalse(AirbnbSync airbnbSync);
    List<AirbnbReservation> findByIsProcessedFalse();
}