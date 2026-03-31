package com.br.elohostel.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.br.elohostel.exceptions.BusinessException;
import com.br.elohostel.model.dtos.PlanRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.mercadopago.client.preapproval.PreapprovalUpdateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preapproval.Preapproval;

@Service
public class MercadoPagoPlanService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPlanService.class);

    @Value("${mercado-pago.access-token}")
    private String accessToken;

    @Value("${mercado-pago.sandbox}")
    private boolean sandbox;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PreapprovalClient preapprovalClient;

    private static final String MP_API_BASE = "https://api.mercadopago.com";
    private static final String PLANS_URL = "/preapproval_plan";

    public MercadoPagoPlanService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.preapprovalClient = new PreapprovalClient();
    }

    public Map<String, Object> createPlan(PlanRequestDTO dto) {
        log.info("Criando plano no MP para: {}", dto.name());
        String url = MP_API_BASE + PLANS_URL;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = new HashMap<>();
        body.put("reason", dto.name());
        body.put("auto_recurring", Map.of(
                "frequency", dto.frequency(),
                "frequency_type", dto.frequencyType(),
                "transaction_amount", dto.price(),
                "currency_id", dto.currency()
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String mpPlanId = root.get("id").asText();
            Map<String, Object> result = new HashMap<>();
            result.put("mpPlanId", mpPlanId);
            result.put("status", root.get("status").asText());
            return result;
        } catch (HttpClientErrorException e) {
            throw new BusinessException("Erro ao criar plano no Mercado Pago: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new BusinessException("Erro inesperado: " + e.getMessage());
        }
    }

    /**
     * Cria uma assinatura utilizando um plano existente (plan_id) e um token de cartão.
     * Este método substitui a versão anterior que usava auto_recurring.
     *
     * @param payerEmail e-mail do pagador
     * @param reason     título da assinatura
     * @param mpPlanId   ID do plano no Mercado Pago (obtido ao criar o plano)
     * @param cardToken  token do cartão gerado no frontend
     * @return Preapproval criado
     */
    public Preapproval createPreapprovalWithToken(String payerEmail, String reason,
                                                  String mpPlanId, String cardToken) {
        log.info("Criando assinatura com plano ID: {}", mpPlanId);
        log.debug("Dados: payerEmail={}, reason={}, cardToken={}", payerEmail, reason, cardToken);
        String url = MP_API_BASE + "/preapproval";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = new HashMap<>();
        body.put("payer_email", payerEmail);
        body.put("reason", reason);
        body.put("plan_id", mpPlanId);          
        body.put("card_token_id", cardToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("Resposta do MP: {}", response.getBody());
            JsonNode root = objectMapper.readTree(response.getBody());
            return objectMapper.treeToValue(root, Preapproval.class);
        } catch (HttpClientErrorException e) {
             log.error("Erro HTTP na chamada ao MP: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("Erro no MP: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro inesperado", e);
            throw new BusinessException("Erro inesperado: " + e.getMessage());
        }
    }

    // Método original usando SDK (sem token, pode ser mantido para outros usos)
    public Preapproval createPreapproval(PreapprovalCreateRequest request) {
        try {
            return preapprovalClient.create(request);
        } catch (MPApiException e) {
            throw new BusinessException("Erro na API do Mercado Pago: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            throw new BusinessException("Erro no SDK do Mercado Pago" + e);
        }
    }

    // Cancelar assinatura via SDK
    public void cancelPreapproval(String id) {
        try {
            PreapprovalUpdateRequest updateRequest = PreapprovalUpdateRequest.builder()
                    .status("cancelled")
                    .build();
            preapprovalClient.update(id, updateRequest);
        } catch (MPApiException | MPException e) {
            throw new BusinessException("Erro ao cancelar assinatura: " + e.getMessage());
        }
    }

    // Buscar assinatura via SDK
    public Preapproval getPreapproval(String id) {
        try {
            return preapprovalClient.get(id);
        } catch (MPApiException | MPException e) {
            throw new RuntimeException("Erro ao buscar assinatura no MP" + e);
        }
    }
}