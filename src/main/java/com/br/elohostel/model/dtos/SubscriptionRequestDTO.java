package com.br.elohostel.model.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubscriptionRequestDTO (
    @NotNull Long planId,
    @NotBlank @Email String payerEmail,
    @NotBlank String cardToken) {
}
