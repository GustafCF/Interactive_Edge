package com.br.elohostel.service.components;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.br.elohostel.model.AirbnbSync;
import com.br.elohostel.model.Tenant;
import com.br.elohostel.repository.AirbnbSyncRepository;
import com.br.elohostel.repository.TenantRepository;
import com.br.elohostel.service.AirbnbICalService;
import com.br.elohostel.util.TenantContext;

@Component
public class AirbnbSyncScheduler {

    private final AirbnbICalService airbnbICalService;
    private final AirbnbSyncRepository airbnbSyncRepository;
    private final TenantRepository tenantRepository;

    public AirbnbSyncScheduler(AirbnbICalService airbnbICalService, 
                             AirbnbSyncRepository airbnbSyncRepository,
                             TenantRepository tenantRepository) {
        this.airbnbICalService = airbnbICalService;
        this.airbnbSyncRepository = airbnbSyncRepository;
        this.tenantRepository = tenantRepository;
    }

    @Scheduled(fixedRate = 900000) // A cada 15 minutos
    public void syncActiveAirbnbCalendars() {
        List<Tenant> allTenants = tenantRepository.findAll();
        
        for (Tenant tenant : allTenants) {
            try {
                TenantContext.setCurrentTenant(tenant.getTenantKey());
                
                List<AirbnbSync> activeSyncs = airbnbSyncRepository.findByIsActiveTrueAndTenant_TenantKey(tenant.getTenantKey());
                
                for (AirbnbSync sync : activeSyncs) {
                    try {
                        airbnbICalService.syncAirbnbReservations(
                            sync.getIcalUrl(), 
                            sync.getPropertyId()
                        );
                    } catch (Exception e) {
                        System.err.println("Erro no sync automático para: " + sync.getPropertyId() + " - " + e.getMessage());
                    }
                }
            } finally {
                TenantContext.clear();
            }
        }
    }
}