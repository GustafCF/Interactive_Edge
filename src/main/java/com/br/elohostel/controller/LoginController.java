package com.br.elohostel.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.dtos.LoginRequestDTO;
import com.br.elohostel.model.dtos.LoginResponseDTO;
import com.br.elohostel.service.LoginService;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private LoginService service;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        LoginResponseDTO obj = service.login(loginRequestDTO);
        return ResponseEntity.ok().body(obj);
    }
}
