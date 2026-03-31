package com.br.elohostel.model.dtos;

import java.math.BigDecimal;

public record PlanResponseDTO (
    Long id,
    String name,
    String description,
    BigDecimal price,
    String currency,
    Integer frequency,
    String frequencyType,
    String mpPlanId
)
{}
