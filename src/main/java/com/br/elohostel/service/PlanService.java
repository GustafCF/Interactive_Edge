package com.br.elohostel.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.br.elohostel.model.Plan;
import com.br.elohostel.model.dtos.PlanRequestDTO;
import com.br.elohostel.model.dtos.PlanResponseDTO;
import com.br.elohostel.repository.PlanRepository;

import jakarta.transaction.Transactional;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final MercadoPagoPlanService mpPlanService;

    public PlanService(PlanRepository planRepository, MercadoPagoPlanService mpPlanService) {
        this.planRepository = planRepository;
        this.mpPlanService = mpPlanService;
    }

    @Transactional
    public PlanResponseDTO createPlan(PlanRequestDTO dto) {
        Plan localPlan = new Plan();
        BeanUtils.copyProperties(dto, localPlan);
        localPlan = planRepository.save(localPlan);
        Map<String, Object> mpResponse = mpPlanService.createPlan(dto);
        String mpPlanId = (String) mpResponse.get("mpPlanId");
        localPlan.setMpPlanId(mpPlanId);
        planRepository.save(localPlan);

        return toResponseDTO(localPlan);
    }

    private PlanResponseDTO toResponseDTO(Plan plan) {
        return new PlanResponseDTO(
            plan.getId(),
            plan.getName(),
            plan.getDescription(),
            plan.getPrice(),
            plan.getCurrency(),
            plan.getFrequency(),
            plan.getFrequencyType(),
            plan.getMpPlanId()
        );
    }

    public List<Plan> findAll() {
      return planRepository.findAll();
    }

    public Plan findById(Long id) {
        Optional<Plan> obj = planRepository.findById(id);
        return obj.get();
    }
}