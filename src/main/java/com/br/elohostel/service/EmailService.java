package com.br.elohostel.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.br.elohostel.model.Email;
import com.br.elohostel.model.enums.StatusEmail;
import com.br.elohostel.repository.EmailRepository;

import jakarta.transaction.Transactional;

@Service
public class EmailService {

	private final EmailRepository repository;
	private final JavaMailSender emailSender;

    public EmailService(EmailRepository repository, JavaMailSender emailSender) {
        this.repository = repository;
        this.emailSender = emailSender;
    }
	
	@Transactional
	@SuppressWarnings("finally")
	public Email sendEmail(Email email) {
		email.setSendDateEmail(LocalDateTime.now());
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(email.getEmailFrom());
            message.setTo(email.getEmailTo());
            message.setSubject(email.getSubject());
            message.setText(email.getText());
            emailSender.send(message);

            email.setStatusEmail(StatusEmail.SENT);
        } catch (MailException e){
            email.setStatusEmail(StatusEmail.ERROR);
        } finally {
            return repository.save(email);
        }
	}
}