package com.br.elohostel.model.dtos;

import java.time.LocalDateTime;

public record SubscriptionResponseDTO (
    Long id,
    String mpPreapprovalId,
    String status,
    LocalDateTime initialDate,
    LocalDateTime finalDate,
    String clienteEmail,
    Long planId,
    String planName
) {
}
