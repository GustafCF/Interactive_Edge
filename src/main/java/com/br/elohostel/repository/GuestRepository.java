package com.br.elohostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.Guest;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    Optional<Guest> findByRg(String rg);
    
    boolean existsByRg(String rg);
    
    List<Guest> findByNameContainingIgnoreCase(String name);
    
    Page<Guest> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Guest> findByName(String name);

    Optional<Guest> findByEmail(String guestEmail);

    List<Guest> findByTenant_TenantKey(String currentTenantKey);

    Optional<Guest> findByIdAndTenant_TenantKey(Long id, String tenantKey);

    Optional<Guest> findByNameAndTenant_TenantKey(String name, String tenantKey);

    Optional<Guest> findByRgAndTenant_TenantKey(String rg, String tenantKey);

    Optional<Guest> findByEmailAndTenant_TenantKey(String email, String tenantKey);

    List<Guest> findByNameContainingIgnoreCaseAndTenant_TenantKey(String name, String tenantKey);

    boolean existsByRgAndTenant_TenantKey(String rg, String tenantKey);

}
