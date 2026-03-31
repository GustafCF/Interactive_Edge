package com.br.elohostel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.dtos.ForgotPasswordRequestDTO;
import com.br.elohostel.model.dtos.LoginRequestDTO;
import com.br.elohostel.model.dtos.LoginResponseDTO;
import com.br.elohostel.model.enums.ResetPasswordRequestDTO;
import com.br.elohostel.service.LoginService;
import com.br.elohostel.service.PasswordResetService;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final LoginService service;
    private final PasswordResetService passwordResetService;

    public LoginController(LoginService service, PasswordResetService passwordResetService) {
        this.service = service;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        LoginResponseDTO obj = service.login(loginRequestDTO);
        return ResponseEntity.ok().body(obj);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.createPasswordResetTokenForUser(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
