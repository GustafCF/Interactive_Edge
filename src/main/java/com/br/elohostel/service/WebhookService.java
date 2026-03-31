package com.br.elohostel.service;

import org.springframework.stereotype.Service;

import com.br.elohostel.repository.SignatureRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class WebhookService {

    private final SignatureRepository signatureRepository;
    private final MercadoPagoPlanService mpService;

    public WebhookService(SignatureRepository signatureRepository, MercadoPagoPlanService mpService) {
        this.signatureRepository = signatureRepository;
        this.mpService = mpService;
    }

    public void processNotification(JsonNode payload) {
        String type = payload.has("type") ? payload.get("type").asText() : "";
        String action = payload.has("action") ? payload.get("action").asText() : "";
        JsonNode data = payload.get("data");
        if (data != null && data.has("id")) {
            String preapprovalId = data.get("id").asText();
            var mpPreapproval = mpService.getPreapproval(preapprovalId);
            signatureRepository.findByMpPreapprovalId(preapprovalId)
                    .ifPresent(signature -> {
                        signature.setStatus(mpPreapproval.getStatus());
                        signatureRepository.save(signature);
                    });
        }
    }
}