package com.br.elohostel.service;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.br.elohostel.exceptions.AccessDeniedException;
import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Role;
import com.br.elohostel.model.dtos.LoginRequestDTO;
import com.br.elohostel.model.dtos.LoginResponseDTO;
import com.br.elohostel.repository.UserRepository;

@Service
public class LoginService {

    private static final Logger logger = Logger.getLogger(LoginService.class.getName());

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public LoginService(JwtEncoder jwtEncoder, UserRepository userRepo, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.jwtEncoder = jwtEncoder;
        this.userRepo = userRepo;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        var user = userRepo.findByEmail(loginRequestDTO.email()).orElseThrow(() -> new ResourceNotFoundException(loginRequestDTO.email()));

        if (!user.loginValidation(loginRequestDTO, bCryptPasswordEncoder)){
            logger.log(Level.SEVERE, "Invalid password!");
            throw new AccessDeniedException();
        }

        var now = Instant.now();
        var expiresIn = 30000L;

        var scopes = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.joining(" "));

        var claims = JwtClaimsSet.builder()
                .issuer("elô_hostel")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scopes)
                .build();

        var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return new LoginResponseDTO(jwtValue, expiresIn);
    }
}