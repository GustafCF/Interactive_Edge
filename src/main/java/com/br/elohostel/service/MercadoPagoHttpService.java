package com.br.elohostel.service;

import java.time.LocalDateTime;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.br.elohostel.model.Plan;
import com.br.elohostel.model.Subscription;
import com.br.elohostel.model.dtos.AutoRecurring;
import com.br.elohostel.model.dtos.PreapprovalRequest;
import com.br.elohostel.model.enums.SubscriptionStatus;
import com.br.elohostel.repository.PlanRepository;
import com.br.elohostel.repository.SubscriptionRepository;
import com.br.elohostel.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MercadoPagoHttpService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoHttpService.class);

    @Value("${mercado-pago.access-token}")
    private String accessToken;

    @Value("${app.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public MercadoPagoHttpService(SubscriptionRepository subscriptionRepository, 
                                  UserRepository userRepository, 
                                  PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    public String createSubscription(Plan plan, String payerEmail, 
                                  String payerName, String customReason) {

        // Usando os valores recebidos como parâmetro, não sobrescrevendo
        String finalPayerEmail = (payerEmail != null && !payerEmail.isEmpty()) 
                                ? payerEmail 
                                : "teste_" + System.currentTimeMillis() + "@email.com";
        String finalPayerName = (payerName != null && !payerName.isEmpty()) 
                               ? payerName 
                               : "Usuario Teste";
        
        AutoRecurring autoRecurring = new AutoRecurring();
        autoRecurring.setFrequency(plan.getFrequency());
        autoRecurring.setFrequencyType(plan.getFrequencyType());
        autoRecurring.setTransactionAmount(plan.getTransactionAmount());
        autoRecurring.setCurrencyId(plan.getCurrencyId() != null ? plan.getCurrencyId() : "BRL");
        
        PreapprovalRequest request = new PreapprovalRequest();
        String reason = customReason != null && !customReason.trim().isEmpty() 
                        ? customReason 
                        : "Assinatura " + plan.getName() + " - " + finalPayerName;
        request.setReason(reason);
        request.setAutoRecurring(autoRecurring);
        request.setPayerEmail(finalPayerEmail);
        request.setBackUrl(baseUrl + "/success");
        request.setStatus("pending");
        
        // LOG DO PAYLOAD
        try {
            String jsonPayload = objectMapper.writeValueAsString(request);
            log.info("Payload sendo enviado ao Mercado Pago: {}", jsonPayload);
        } catch (Exception e) {
            log.warn("Não foi possível serializar payload para log: {}", e.getMessage());
        }
        
        // CONFIGURAR HEADERS
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        
        log.info("Authorization header configurado: Bearer {}...", 
                accessToken.substring(0, Math.min(15, accessToken.length())));
        
        // EXECUTAR REQUISIÇÃO
        HttpEntity<PreapprovalRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            log.info("Enviando requisição POST para: https://api.mercadopago.com/preapproval");
            
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.mercadopago.com/preapproval",
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            log.info("Resposta recebida com status code: {}", response.getStatusCode());
            
            if (response.getBody() == null) {
                log.error("Resposta do Mercado Pago está vazia");
                throw new RuntimeException("Resposta vazia do Mercado Pago");
            }
            
            Map<String, Object> responseBody = response.getBody();
            log.info("Response body: {}", responseBody);
            
            // EXTRAIR DADOS DA RESPOSTA
            String initPoint = (String) responseBody.get("init_point");
            String preapprovalId = (String) responseBody.get("id");
            String collectorId = responseBody.get("collector_id") != null ? 
                        String.valueOf(responseBody.get("collector_id")) : null;
            String nextPaymentDate = (String) responseBody.get("next_payment_date");
            
            // CORREÇÃO: O Subscription precisa de um Tenant
            // Para teste, crie um tenant dummy ou busque um existente
            // Como você disse que é só teste, vou comentar a parte do banco por enquanto
            
            /*
            Subscription subscription = new Subscription();
            subscription.setPlan(plan);
            subscription.setMercadoPagoPreapprovalId(preapprovalId);
            subscription.setMercadoPagoCollectorId(collectorId);
            subscription.setActive(true);
            subscription.setStatus(SubscriptionStatus.PENDING);
            subscription.setCreatedAt(LocalDateTime.now());
            subscription.setUpdatedAt(LocalDateTime.now());
            
            if (nextPaymentDate != null && !nextPaymentDate.isEmpty()) {
                try {
                    subscription.setNextPaymentDate(LocalDateTime.parse(nextPaymentDate));
                } catch (Exception e) {
                    log.warn("Não foi possível parsear next_payment_date: {}", nextPaymentDate);
                }
            }
            
            subscriptionRepository.save(subscription);
            log.info("Assinatura salva no banco de dados com ID: {}", subscription.getId());
            */
            
            log.info("Assinatura criada com sucesso! Preapproval ID: {}, Init Point: {}", preapprovalId, initPoint);
            return initPoint;
            
        } catch (HttpClientErrorException e) {
            // TRATAMENTO DE ERROS HTTP 4xx - Agora lançando exceção
            log.error("=== ERRO HTTP 4xx NA REQUISIÇÃO ===");
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Status Text: {}", e.getStatusText());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Erro na requisição ao Mercado Pago: " + e.getResponseBodyAsString(), e);
            
        } catch (HttpServerErrorException e) {
            // TRATAMENTO DE ERROS HTTP 5xx
            log.error("=== ERRO HTTP 5xx NA REQUISIÇÃO ===");
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Erro no servidor do Mercado Pago. Tente novamente mais tarde.", e);
            
        } catch (Exception e) {
            // TRATAMENTO DE ERROS GERAIS
            log.error("=== ERRO INESPERADO AO CRIAR ASSINATURA ===");
            log.error("Tipo do erro: {}", e.getClass().getName());
            log.error("Mensagem: {}", e.getMessage());
            log.error("Stack trace completo:", e);
            throw new RuntimeException("Erro interno ao criar assinatura: " + e.getMessage(), e);
        }
    }
}