package com.br.elohostel.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.Plan;
import com.br.elohostel.repository.PlanRepository;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanRepository planRepository;

    public PlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    public ResponseEntity<List<Plan>> listarPlanos() {
        return ResponseEntity.ok(planRepository.findByIsActiveTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Plan> buscarPlano(@PathVariable Long id) {
        return planRepository.findByIdAndIsActiveTrue(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> criarPlano(@RequestBody Plan plan) {
        try {
            if (planRepository.findByName(plan.getName()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Plano com este nome já existe"));
            }
            
            plan.setCreatedAt(LocalDateTime.now());
            plan.setIsActive(true);
            Plan saved = planRepository.save(plan);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao criar plano: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarPlano(@PathVariable Long id, @RequestBody Plan planAtualizado) {
        return planRepository.findById(id).map(plan -> {
            plan.setName(planAtualizado.getName());
            plan.setDescription(planAtualizado.getDescription());
            plan.setFrequency(planAtualizado.getFrequency());
            plan.setFrequencyType(planAtualizado.getFrequencyType());
            plan.setTransactionAmount(planAtualizado.getTransactionAmount());
            plan.setCurrencyId(planAtualizado.getCurrencyId());
            plan.setMaxRooms(planAtualizado.getMaxRooms());
            plan.setMaxBeds(planAtualizado.getMaxBeds());
            plan.setIsActive(planAtualizado.getIsActive());
            plan.setUpdatedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(planRepository.save(plan));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> desativarPlano(@PathVariable Long id) {
        return planRepository.findById(id).map(plan -> {
            plan.setIsActive(false);
            plan.setUpdatedAt(LocalDateTime.now());
            planRepository.save(plan);
            return ResponseEntity.ok(Map.of("message", "Plano desativado com sucesso"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
