package com.br.elohostel.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.Plan;
import com.br.elohostel.model.dtos.PlanRequestDTO;
import com.br.elohostel.model.dtos.PlanResponseDTO;
import com.br.elohostel.service.PlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/createPlan")
    public ResponseEntity<PlanResponseDTO> createPlan(@Valid @RequestBody PlanRequestDTO dto) {
        PlanResponseDTO response = planService.createPlan(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Plan>> getAllPlans() {
        List<Plan> plans = planService.findAll();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Plan> getPlanById(@PathVariable Long id) {
        Plan plan = planService.findById(id);
        return ResponseEntity.ok(plan);
    }
}