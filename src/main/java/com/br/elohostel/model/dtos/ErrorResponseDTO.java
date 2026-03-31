package com.br.elohostel.model.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDTO(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details
) {}