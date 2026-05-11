package com.br.elohostel.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.Plan;
import com.br.elohostel.model.Tenant;
import com.br.elohostel.model.dtos.SubscriptionRequest;
import com.br.elohostel.repository.PlanRepository;
import com.br.elohostel.repository.TenantRepository;
import com.br.elohostel.service.MercadoPagoHttpService;

@RestController
@RequestMapping("/mp")
public class MpController {

    private static final Logger log = LoggerFactory.getLogger(MpController.class);

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final MercadoPagoHttpService mercadoPagoService;

    public MpController(TenantRepository tenantRepository, 
                        PlanRepository planRepository,
                        MercadoPagoHttpService mercadoPagoService) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.mercadoPagoService = mercadoPagoService;
    }

    @PostMapping("/subscription/create")
    public ResponseEntity<?> criarAssinatura(@RequestBody SubscriptionRequest request) {
        try {
            log.info("Recebendo requisição de criação de assinatura: {}", request);
            
            // Se não veio planId, cria um plano temporário para teste
            Plan plan;
            if (request.getPlanId() != null) {
                plan = planRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plano não encontrado"));
            } else {
                // Cria um plano temporário apenas para teste (não salva no banco)
                plan = new Plan();
                plan.setName(request.getPlanName() != null ? request.getPlanName() : "Plano Teste");
                plan.setDescription("Plano criado para teste");
                plan.setFrequency(request.getFrequency() != null ? request.getFrequency() : 1);
                plan.setFrequencyType(request.getFrequencyType() != null ? request.getFrequencyType() : "months");
                plan.setTransactionAmount(request.getTransactionAmount() != null ? request.getTransactionAmount() : new BigDecimal("59.90"));
                plan.setCurrencyId(request.getCurrencyId() != null ? request.getCurrencyId() : "BRL");
                plan.setIsActive(true);
                plan.setCreatedAt(LocalDateTime.now());
            }
            
            // Dados de teste fixos
            String payerEmail = request.getPayerEmail() != null ? request.getPayerEmail() : "teste_" + System.currentTimeMillis() + "@email.com";
            String payerName = request.getPayerName() != null ? request.getPayerName() : "Usuario Teste";
            String reason = request.getReason() != null ? request.getReason() : "Assinatura Teste - " + plan.getName();
            
            // Chama o serviço para criar a assinatura
            String initPoint = mercadoPagoService.createSubscription(plan, payerEmail, payerName, reason);
            
            // Retorna a URL de checkout
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("init_point", initPoint);
            response.put("message", "Assinatura criada com sucesso. Acesse o link para finalizar o pagamento.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao criar assinatura: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    // Endpoint de teste simplificado - NÃO precisa de SubscriptionRequest
    @PostMapping("/subscription/test")
    public ResponseEntity<?> testarAssinatura() {
        try {
            log.info("=== INICIANDO TESTE DE ASSINATURA ===");
            
            // Criar plano de teste
            Plan planoTeste = new Plan();
            planoTeste.setName("Plano Teste Rápido");
            planoTeste.setDescription("Plano para validação da integração");
            planoTeste.setFrequency(1);
            planoTeste.setFrequencyType("months");
            planoTeste.setTransactionAmount(new BigDecimal("59.90"));
            planoTeste.setCurrencyId("BRL");
            planoTeste.setIsActive(true);
            planoTeste.setCreatedAt(LocalDateTime.now());
            
            // Dados de teste
            String emailTeste = "teste_" + System.currentTimeMillis() + "@mercadopago.com";
            String nomeTeste = "Usuario Teste";
            String razaoTeste = "Assinatura Teste - Validação API";
            
            log.info("Criando assinatura com:");
            log.info("  Plano: {}", planoTeste.getName());
            log.info("  Valor: R$ {}", planoTeste.getTransactionAmount());
            log.info("  Email: {}", emailTeste);
            log.info("  Razão: {}", razaoTeste);
            
            // Chamar o serviço
            String initPoint = mercadoPagoService.createSubscription(planoTeste, emailTeste, nomeTeste, razaoTeste);
            
            // Montar resposta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("init_point", initPoint);
            response.put("message", "Assinatura de teste criada com sucesso!");
            response.put("test_data", Map.of(
                "email", emailTeste,
                "plan_name", planoTeste.getName(),
                "amount", planoTeste.getTransactionAmount()
            ));
            
            log.info("=== TESTE FINALIZADO COM SUCESSO ===");
            log.info("URL de pagamento: {}", initPoint);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("=== ERRO NO TESTE ===");
            log.error("Mensagem: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("error_type", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/subscription/plans")
    public ResponseEntity<List<Plan>> listarPlanos() {
        return ResponseEntity.ok(planRepository.findByIsActiveTrue());
    }
    
    @PostMapping("/subscription/plan/create")
    public ResponseEntity<?> criarPlano(@RequestBody Plan plan) {
        plan.setCreatedAt(java.time.LocalDateTime.now());
        plan.setIsActive(true);
        Plan saved = planRepository.save(plan);
        return ResponseEntity.ok(saved);
    }

}