package com.br.elohostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.AirbnbSync;

@Repository
public interface AirbnbSyncRepository extends JpaRepository<AirbnbSync, Long> {

    boolean existsByPropertyIdAndTenant_TenantKey(String propertyId, String tenantKey);

    List<AirbnbSync> findByIsActiveTrueAndTenant_TenantKey(String tenantKey);
    
    @Query("SELECT a FROM AirbnbSync a WHERE a.icalUrl = :icalUrl AND a.propertyId = :propertyId AND a.tenant.tenantKey = :tenantKey")
    Optional<AirbnbSync> findByIcalUrlAndPropertyIdAndTenantKey(
        @Param("icalUrl") String icalUrl, 
        @Param("propertyId") String propertyId,
        @Param("tenantKey") String tenantKey
    );

    Optional<AirbnbSync> findByPropertyIdAndTenant_TenantKey(String propertyId, String tenantKey);

    Optional<AirbnbSync> findByIdAndTenant_TenantKey(Long id, String tenantKey);

    List<AirbnbSync> findByTenant_TenantKey(String tenantKey);
}