package com.br.elohostel.model.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlanRequestDTO(
    @NotBlank String name,
    String description,
    @NotNull @Positive BigDecimal price,
    @NotBlank String currency,
    @NotNull @Positive Integer frequency,
    @NotBlank String frequencyType) {}
