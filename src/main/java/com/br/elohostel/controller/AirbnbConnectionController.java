package com.br.elohostel.controller;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.AirbnbSync;
import com.br.elohostel.model.dtos.AirbnbConnectionRequest;
import com.br.elohostel.repository.AirbnbSyncRepository;
import com.br.elohostel.service.AirbnbICalService;

@RestController
@RequestMapping("/api/airbnb/sync")
public class AirbnbConnectionController {

    private final AirbnbICalService airbnbICalService;
    private final AirbnbSyncRepository airbnbSyncRepository;

    public AirbnbConnectionController(AirbnbICalService airbnbICalService,
                                    AirbnbSyncRepository airbnbSyncRepository) {
        this.airbnbICalService = airbnbICalService;
        this.airbnbSyncRepository = airbnbSyncRepository;
    }

    /**
     * Endpoint para configurar a conexão com o Airbnb
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connectAirbnb(
            @RequestBody AirbnbConnectionRequest request) {
        
        try {
            // Validar a URL iCal
            if (!isValidICalUrl(request.icalUrl())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "URL iCal inválida"));
            }

            // Testar a conexão com o calendário
            if (!testICalConnection(request.icalUrl())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Não foi possível acessar o calendário iCal"));
            }

            // Fazer primeira sincronização
            airbnbICalService.syncAirbnbReservations(request.icalUrl(), request.propertyId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Conexão com Airbnb configurada com sucesso");
            response.put("propertyId", request.propertyId());
            response.put("status", "CONNECTED");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro na conexão: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para listar conexões configuradas
     */
    @GetMapping("/connections")
    public ResponseEntity<List<AirbnbSync>> getConnections() {
        List<AirbnbSync> connections = airbnbSyncRepository.findByIsActiveTrue();
        return ResponseEntity.ok(connections);
    }

    /**
     * Endpoint para atualizar URL iCal
     */
    @PutMapping("/connections/{id}")
    public ResponseEntity<Map<String, Object>> updateConnection(
            @PathVariable Long id,
            @RequestBody AirbnbConnectionRequest request) {
        
        Optional<AirbnbSync> existingSync = airbnbSyncRepository.findById(id);
        
        if (existingSync.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AirbnbSync sync = existingSync.get();
        sync.setIcalUrl(request.icalUrl());
        sync.setPropertyId(request.propertyId());
        sync.setLastSync(LocalDateTime.now());

        airbnbSyncRepository.save(sync);

        // Fazer nova sincronização
        airbnbICalService.syncAirbnbReservations(request.icalUrl(), request.propertyId());

        return ResponseEntity.ok(Map.of("message", "Conexão atualizada com sucesso"));
    }

    /**
     * Endpoint para desativar conexão
     */
    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Map<String, Object>> disableConnection(@PathVariable Long id) {
        Optional<AirbnbSync> existingSync = airbnbSyncRepository.findById(id);
        
        if (existingSync.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AirbnbSync sync = existingSync.get();
        sync.setIsActive(false);
        airbnbSyncRepository.save(sync);

        return ResponseEntity.ok(Map.of("message", "Conexão desativada com sucesso"));
    }

    /**
     * Endpoint para sincronização manual
     */
    @PostMapping("/sync-now")
    public ResponseEntity<Map<String, Object>> syncNow() {
        try {
            List<AirbnbSync> activeConnections = airbnbSyncRepository.findByIsActiveTrue();
            
            int syncedCount = 0;
            List<String> results = new ArrayList<>();

            for (AirbnbSync connection : activeConnections) {
                try {
                    airbnbICalService.syncAirbnbReservations(
                        connection.getIcalUrl(), 
                        connection.getPropertyId()
                    );
                    syncedCount++;
                    results.add("✓ " + connection.getPropertyId() + " - Sincronizado");
                } catch (Exception e) {
                    results.add("✗ " + connection.getPropertyId() + " - Erro: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Sincronização manual concluída");
            response.put("syncedCount", syncedCount);
            response.put("totalConnections", activeConnections.size());
            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro na sincronização: " + e.getMessage()));
        }
    }

    private boolean isValidICalUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Verificar se é uma URL válida e termina com .ics
        return url.matches("^https?://.+\\.ics(\\?.*)?$");
    }

    private boolean testICalConnection(String icalUrl) {
        try {
            URL url = new URL(icalUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
            
        } catch (Exception e) {
            return false;
        }
    }
}