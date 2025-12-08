package com.br.elohostel.service.components;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.br.elohostel.model.AirbnbSync;
import com.br.elohostel.repository.AirbnbSyncRepository;
import com.br.elohostel.service.AirbnbICalService;

@Component
public class AirbnbSyncScheduler {

    private final AirbnbICalService airbnbICalService;
    private final AirbnbSyncRepository airbnbSyncRepository;

    public AirbnbSyncScheduler(AirbnbICalService airbnbICalService, 
                             AirbnbSyncRepository airbnbSyncRepository) {
        this.airbnbICalService = airbnbICalService;
        this.airbnbSyncRepository = airbnbSyncRepository;
    }

    @Scheduled(fixedRate = 900000) // A cada 15 minutos
    public void syncActiveAirbnbCalendars() {
        List<AirbnbSync> activeSyncs = airbnbSyncRepository.findByIsActiveTrue();
        
        for (AirbnbSync sync : activeSyncs) {
            try {
                airbnbICalService.syncAirbnbReservations(
                    sync.getIcalUrl(), 
                    sync.getPropertyId()
                );
            } catch (Exception e) {
                System.err.println("Erro no sync automático para: " + sync.getPropertyId());
            }
        }
    }
}