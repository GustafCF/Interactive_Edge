package com.br.elohostel.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.dtos.SubscriptionRequestDTO;
import com.br.elohostel.model.dtos.SubscriptionResponseDTO;
import com.br.elohostel.service.SubscriptionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/create-sub")
    public ResponseEntity<SubscriptionResponseDTO> createSubscription(@Valid @RequestBody SubscriptionRequestDTO dto) {
        SubscriptionResponseDTO response = subscriptionService.createSubscription(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long id) {
        subscriptionService.cancelSubscription(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponseDTO> getSubscription(@PathVariable Long id) {
        SubscriptionResponseDTO response = subscriptionService.findById(id);
        return ResponseEntity.ok(response);
    }
}