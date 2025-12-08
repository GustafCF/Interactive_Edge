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

    // Optional<AirbnbSync> findByPropertyId(String propertyId);
    // List<AirbnbSync> findByIsActiveTrue();
    boolean existsByPropertyId(String propertyId);

    List<AirbnbSync> findByIsActiveTrue();
    
    // CORREÇÃO: Adicionar método para buscar por URL e property
    @Query("SELECT a FROM AirbnbSync a WHERE a.icalUrl = :icalUrl AND a.propertyId = :propertyId")
    Optional<AirbnbSync> findByIcalUrlAndPropertyId(@Param("icalUrl") String icalUrl, 
                                                   @Param("propertyId") String propertyId);

     // ADICIONE ESTE MÉTODO:
    Optional<AirbnbSync> findByPropertyId(String propertyId);                                               
                                                   
}