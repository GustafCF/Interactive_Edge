package com.br.elohostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.Bed;
import com.br.elohostel.model.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByNumber(Integer number);
    
    Optional<Room> findByNumberAndTenant_TenantKey(Integer number, String tenantKey);

    List<Room> findByTenant_TenantKey(String currentTenantKey);

    Optional<Room> findByIdAndTenant_TenantKey(Long id, String tenantKey);
}
