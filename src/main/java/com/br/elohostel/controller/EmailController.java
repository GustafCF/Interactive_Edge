package com.br.elohostel.controller;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.dtos.EmailDto;
import com.br.elohostel.model.Email;
import com.br.elohostel.service.EmailService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/ms-emails")
public class EmailController {

	@Autowired
	private EmailService service;

	@PostMapping(value = "/sending-email")
	public ResponseEntity<Email> sendingEmail(@RequestBody @Valid EmailDto emailDto) {
		Email emailModel = new Email();
		BeanUtils.copyProperties(emailDto, emailModel);
		service.sendEmail(emailModel);
		return new ResponseEntity<>(emailModel, HttpStatus.CREATED);
	}
}