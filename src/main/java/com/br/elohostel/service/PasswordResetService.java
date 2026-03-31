package com.br.elohostel.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Email;
import com.br.elohostel.model.PasswordResetToken;
import com.br.elohostel.model.User;
import com.br.elohostel.repository.PasswordResetTokenRepository;
import com.br.elohostel.repository.UserRepository;

import jakarta.transaction.Transactional;
                
@Service
public class PasswordResetService {

   private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public void createPasswordResetTokenForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com e-mail: " + email));
        tokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);
        String resetLink = baseUrl + "/login?token=" + token;
        sendResetEmail(user.getEmail(), resetLink);
    }

    private void sendResetEmail(String email, String link) {
        Email emailModel = new Email();
        emailModel.setEmailFrom("softwise.system.of@gmail.com");
        emailModel.setEmailTo(email);
        emailModel.setSubject("Recuperação de Senha");
        emailModel.setText("Clique no link para redefinir sua senha: " + link);
        emailService.sendEmail(emailModel);
    }

   @Transactional
    public void resetPassword(String token, String newPassword) {
        logger.info("Tentando redefinir senha com token: {}", token);
        
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("Token não encontrado: {}", token);
                    return new IllegalArgumentException("Token inválido");
                });

        logger.info("Token encontrado. Usuário: {}, Expira em: {}, Usado: {}", 
            resetToken.getUser().getEmail(), 
            resetToken.getExpiryDate(), 
            resetToken.isUsed());

        if (!resetToken.isValid()) {
            logger.warn("Token inválido ou expirado. Expira em: {}, Usado: {}", 
                resetToken.getExpiryDate(), 
                resetToken.isUsed());
            throw new IllegalArgumentException("Token expirado ou já utilizado");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        
        logger.info("Senha redefinida com sucesso para o usuário: {}", user.getEmail());
    }
}