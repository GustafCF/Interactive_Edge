package com.br.elohostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.AirbnbReservation;
import com.br.elohostel.model.AirbnbSync;

@Repository
public interface AirbnbReservationRepository extends JpaRepository<AirbnbReservation, Long> {

    Optional<AirbnbReservation> findByAirbnbReservationIdAndTenant_TenantKey(String airbnbReservationId, String tenantKey);

    @Query("SELECT ar FROM AirbnbReservation ar WHERE ar.airbnbSync = :airbnbSync AND ar.isProcessed = false AND ar.tenant.tenantKey = :tenantKey")
    List<AirbnbReservation> findByAirbnbSyncAndIsProcessedFalse(@Param("airbnbSync") AirbnbSync airbnbSync, @Param("tenantKey") String tenantKey);

    @Query("SELECT ar FROM AirbnbReservation ar WHERE ar.isProcessed = false AND ar.tenant.tenantKey = :tenantKey")
    List<AirbnbReservation> findByIsProcessedFalseAndTenantKey(@Param("tenantKey") String tenantKey);

    Optional<AirbnbReservation> findByIdAndTenant_TenantKey(Long id, String tenantKey);

    List<AirbnbReservation> findByTenant_TenantKey(String tenantKey);
}