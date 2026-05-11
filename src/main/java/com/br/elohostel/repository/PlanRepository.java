package com.br.elohostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.Plan;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByName(String name);
    
    List<Plan> findByIsActiveTrue();
    
    Optional<Plan> findByIsActiveTrueAndId(Long id);

    Optional<Plan> findByIdAndIsActiveTrue(Long id);

}
