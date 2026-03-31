package com.br.elohostel.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.service.WebhookService;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/webhooks/mercadopago")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/notfy")
    public ResponseEntity<String> handleNotification(@RequestBody JsonNode payload) {
        log.info("Webhook recebido: {}", payload);
        try {
            webhookService.processNotification(payload);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Erro ao processar webhook", e);
            return ResponseEntity.status(500).body("Erro interno");
        }
    }
}