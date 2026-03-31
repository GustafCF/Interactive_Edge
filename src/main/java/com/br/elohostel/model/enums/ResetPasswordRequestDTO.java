package com.br.elohostel.model.enums;

public record ResetPasswordRequestDTO(String token, String newPassword) {
    
}