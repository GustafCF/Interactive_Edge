package com.br.elohostel.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.br.elohostel.exceptions.BusinessException;
import com.br.elohostel.model.Plan;
import com.br.elohostel.model.Signature;
import com.br.elohostel.model.dtos.SubscriptionRequestDTO;
import com.br.elohostel.model.dtos.SubscriptionResponseDTO;
import com.br.elohostel.repository.PlanRepository;
import com.br.elohostel.repository.SignatureRepository;
import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest;
import com.mercadopago.resources.preapproval.Preapproval;
import com.mercadopago.resources.preapproval.PreapprovalAutoRecurring;

import jakarta.transaction.Transactional;

@Service
public class SubscriptionService {

    private final SignatureRepository signatureRepository;
    private final PlanRepository planRepository;
    private final MercadoPagoPlanService mpService;

    public SubscriptionService(SignatureRepository signatureRepository,
                               PlanRepository planRepository,
                               MercadoPagoPlanService mpService) {
        this.signatureRepository = signatureRepository;
        this.planRepository = planRepository;
        this.mpService = mpService;
    }

    @Transactional
    public SubscriptionResponseDTO createSubscription(SubscriptionRequestDTO dto) {
        Plan plan = planRepository.findById(dto.planId())
                .orElseThrow(() -> new BusinessException("Plano não encontrado"));

        // Verifica se o plano possui um ID válido no Mercado Pago
        if (plan.getMpPlanId() == null || plan.getMpPlanId().isEmpty()) {
            throw new BusinessException("Plano não está sincronizado com o Mercado Pago");
        }

        // Cria a assinatura usando o plan_id do Mercado Pago
        Preapproval mpPreapproval = mpService.createPreapprovalWithToken(
                dto.payerEmail(),
                plan.getName(),
                plan.getMpPlanId(),          // ID do plano no MP
                dto.cardToken()
        );

        // Extrai as datas do objeto auto_recurring (se houver)
        PreapprovalAutoRecurring recurring = mpPreapproval.getAutoRecurring();
        LocalDateTime initDate = recurring != null && recurring.getStartDate() != null ?
                recurring.getStartDate().toLocalDateTime() : null;
        LocalDateTime finalDate = recurring != null && recurring.getEndDate() != null ?
                recurring.getEndDate().toLocalDateTime() : null;

        // Se startDate não foi definido, use a data de criação da assinatura
        if (initDate == null && mpPreapproval.getDateCreated() != null) {
            initDate = mpPreapproval.getDateCreated().toLocalDateTime();
        }

        Signature signature = new Signature();
        signature.setMpPreapprovalId(mpPreapproval.getId());
        signature.setStatus(mpPreapproval.getStatus());
        signature.setInitialDate(initDate);
        signature.setFinalDate(finalDate);
        signature.setClienteEmail(dto.payerEmail());
        signature.setPlan(plan);

        signature = signatureRepository.save(signature);

        return toResponseDTO(signature);
    }

    @Transactional
    public void cancelSubscription(Long id) {
        Signature signature = signatureRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Assinatura não encontrada"));

        mpService.cancelPreapproval(signature.getMpPreapprovalId());

        signature.setStatus("cancelled");
        signatureRepository.save(signature);
    }

    private SubscriptionResponseDTO toResponseDTO(Signature signature) {
        return new SubscriptionResponseDTO(
                signature.getId(),
                signature.getMpPreapprovalId(),
                signature.getStatus(),
                signature.getInitialDate(),
                signature.getFinalDate(),
                signature.getClienteEmail(),
                signature.getPlan().getId(),
                signature.getPlan().getName()
        );
    }

    public SubscriptionResponseDTO findById(Long id) {
        Signature signature = signatureRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Assinatura não encontrada"));
        return toResponseDTO(signature);
    }
}