package com.br.elohostel.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
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

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.AirbnbSync;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.dtos.AirbnbBidirectionalRequest;
import com.br.elohostel.repository.AirbnbSyncRepository;
import com.br.elohostel.repository.RoomRepository;
import com.br.elohostel.service.AirbnbICalService;
import com.br.elohostel.service.ICalExportService;

@RestController
@RequestMapping("/api/airbnb") 
public class AirbnbBidirectionalController {

    private static final Logger logger = Logger.getLogger(AirbnbBidirectionalController.class.getName());

    @Value("${app.base-url}")
    private String baseUrl;

    private final AirbnbICalService airbnbICalService;
    private final AirbnbSyncRepository airbnbSyncRepository;
    private final ICalExportService icalExportService;

    private final RoomRepository roomRepository; // ✅ ADICIONE ESTA INJEÇÃO

    public AirbnbBidirectionalController(AirbnbICalService airbnbICalService,
                                    AirbnbSyncRepository airbnbSyncRepository,
                                    ICalExportService icalExportService,
                                    RoomRepository roomRepository) { // ✅ ADICIONE NO CONSTRUTOR
        this.airbnbICalService = airbnbICalService;
        this.airbnbSyncRepository = airbnbSyncRepository;
        this.icalExportService = icalExportService;
        this.roomRepository = roomRepository; // ✅ INICIALIZE
    }

    /**
     * Endpoint para verificar status da API
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Airbnb Bidirectional Sync");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("baseUrl", baseUrl);
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    /**
     * Configuração completa bidirecional
     */
   @PostMapping("/setup-bidirectional")
public ResponseEntity<Map<String, Object>> setupBidirectional(
        @RequestBody AirbnbBidirectionalRequest request) {
    
    try {
        // 1. Validar URL do Airbnb
        if (!isValidAirbnbICalUrl(request.getAirbnbIcalUrl())) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("URL iCal do Airbnb inválida. Deve ser uma URL válida do Airbnb calendar."));
        }

        // 2. Verificar se já existe configuração para este propertyId
        Optional<AirbnbSync> existingConfig = airbnbSyncRepository.findByPropertyId(request.getPropertyId());
        if (existingConfig.isPresent()) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Já existe uma configuração para o Property ID: " + request.getPropertyId()));
        }

        // ✅ NOVO: Buscar o quarto se roomId foi fornecido
        Room associatedRoom = null;
        if (request.getRoomId() != null) {
            try {
                associatedRoom = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado com ID: " + request.getRoomId()));
                logger.info("🔗 Quarto associado: " + associatedRoom.getNumber() + " (ID: " + associatedRoom.getId() + ")");
            } catch (ResourceNotFoundException e) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Quarto não encontrado com ID: " + request.getRoomId()));
            }
        } else if (request.getRoomNumber() != null) {
            // ✅ Tentar buscar pelo número do quarto se roomId não foi fornecido
            try {
                associatedRoom = roomRepository.findByNumber(request.getRoomNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado com número: " + request.getRoomNumber()));
                logger.info("🔗 Quarto encontrado pelo número: " + associatedRoom.getNumber());
            } catch (ResourceNotFoundException e) {
                logger.warning("⚠️ Quarto com número " + request.getRoomNumber() + " não encontrado. Criando sync sem associação direta.");
            }
        }

        // 3. Gerar URL de exportação
        String exportUrl = icalExportService.generateExportUrl(request.getPropertyId());
        String fullExportUrl = baseUrl + exportUrl;

        // 4. Salvar configuração
        AirbnbSync syncConfig = new AirbnbSync();
        syncConfig.setIcalUrl(request.getAirbnbIcalUrl());
        syncConfig.setExportIcalUrl(fullExportUrl);
        syncConfig.setPropertyId(request.getPropertyId());
        syncConfig.setPropertyName(request.getPropertyName());
        syncConfig.setCalendarName(request.getCalendarName());
        
        // ✅ ASSOCIAR O QUARTO
        syncConfig.setRoom(associatedRoom);
        syncConfig.setRoomNumber(request.getRoomNumber());
        syncConfig.setSyncDirection(AirbnbSync.SyncDirection.BIDIRECTIONAL);
        syncConfig.setIsActive(true);
        syncConfig.setLastSync(LocalDateTime.now());

        AirbnbSync savedConfig = airbnbSyncRepository.save(syncConfig);

        // 5. Fazer primeira sincronização em background
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("🔄 Executando sincronização inicial para: " + request.getPropertyId());
                airbnbICalService.syncAirbnbReservations(request.getAirbnbIcalUrl(), request.getPropertyId());
                logger.info("✅ Sincronização inicial concluída para: " + request.getPropertyId());
            } catch (Exception e) {
                logger.severe("❌ Erro na sincronização inicial para " + request.getPropertyId() + ": " + e.getMessage());
            }
        });

        Map<String, Object> response = createSuccessResponse(
            "Configuração bidirecional concluída com sucesso!",
            request,
            fullExportUrl,
            savedConfig
        );

        // ✅ ADICIONAR INFORMAÇÕES DO QUARTO NA RESPOSTA
        if (associatedRoom != null) {
            response.put("roomAssociated", true);
            response.put("roomNumber", associatedRoom.getNumber());
            response.put("roomId", associatedRoom.getId());
        } else {
            response.put("roomAssociated", false);
            response.put("roomNumber", request.getRoomNumber());
        }

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        logger.severe("❌ Erro na configuração bidirecional: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("Erro na configuração: " + e.getMessage()));
    }
}

    /**
     * Configuração específica para calendário frontend
     */
    @PostMapping("/setup-calendar-bidirectional")
    public ResponseEntity<Map<String, Object>> setupCalendarBidirectional(
            @RequestBody AirbnbBidirectionalRequest request) {
        
        try {
            // 1. Validar URL do Airbnb
            if (!isValidAirbnbICalUrl(request.getAirbnbIcalUrl())) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("URL iCal do Airbnb inválida."));
            }

            // 2. Verificar se já existe configuração
            Optional<AirbnbSync> existingConfig = airbnbSyncRepository.findByPropertyId(request.getPropertyId());
            if (existingConfig.isPresent()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Já existe uma configuração para o Property ID: " + request.getPropertyId()));
            }

            // 3. Gerar URL específica para calendário
            String exportUrl = icalExportService.generateCalendarExportUrl(request.getPropertyId());
            String fullExportUrl = baseUrl + exportUrl;

            // 4. Salvar configuração
            AirbnbSync syncConfig = new AirbnbSync();
            syncConfig.setIcalUrl(request.getAirbnbIcalUrl());
            syncConfig.setExportIcalUrl(fullExportUrl);
            syncConfig.setPropertyId(request.getPropertyId());
            syncConfig.setPropertyName(request.getPropertyName());
            syncConfig.setCalendarName(request.getCalendarName());
            syncConfig.setSyncDirection(AirbnbSync.SyncDirection.BIDIRECTIONAL);
            syncConfig.setIsActive(true);
            syncConfig.setLastSync(LocalDateTime.now());

            AirbnbSync savedConfig = airbnbSyncRepository.save(syncConfig);

            // 5. Sincronização inicial em background
            CompletableFuture.runAsync(() -> {
                try {
                    airbnbICalService.syncAirbnbReservations(request.getAirbnbIcalUrl(), request.getPropertyId());
                    System.out.println("Sincronização inicial do calendário concluída para: " + request.getPropertyId());
                } catch (Exception e) {
                    System.err.println("Erro na sincronização inicial do calendário para " + request.getPropertyId() + ": " + e.getMessage());
                }
            });

            Map<String, Object> response = createSuccessResponse(
                "Calendário conectado com Airbnb com sucesso!",
                request,
                fullExportUrl,
                savedConfig
            );
            
            response.put("calendarType", "FRONTEND_CALENDAR");
            response.put("syncFrequency", "A cada 15 minutos");
            response.put("nextSteps", getCalendarNextSteps(fullExportUrl, request.getCalendarName()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro na configuração: " + e.getMessage()));
        }
    }

    /**
     * Listar todas as conexões ativas
     */
    @GetMapping("/connections")
    public ResponseEntity<List<AirbnbSync>> getConnections() {
        List<AirbnbSync> connections = airbnbSyncRepository.findByIsActiveTrue();
        return ResponseEntity.ok(connections);
    }

    /**
     * Buscar conexão específica por propertyId
     */
    @GetMapping("/connections/{propertyId}")
    public ResponseEntity<Map<String, Object>> getConnection(@PathVariable String propertyId) {
        Optional<AirbnbSync> connection = airbnbSyncRepository.findByPropertyId(propertyId);
        
        if (connection.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("connection", connection.get());
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse("Conexão não encontrada para: " + propertyId));
        }
    }

    /**
     * Sincronização manual
     */
    @PostMapping("/sync-now/{propertyId}")
    public ResponseEntity<Map<String, Object>> syncNow(@PathVariable String propertyId) {
        try {
            Optional<AirbnbSync> syncConfig = airbnbSyncRepository.findByPropertyId(propertyId);
            if (syncConfig.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Configuração não encontrada para: " + propertyId));
            }

            // Executar sincronização
            airbnbICalService.syncAirbnbReservations(
                syncConfig.get().getIcalUrl(), 
                propertyId
            );

            // Atualizar último sync
            syncConfig.get().setLastSync(LocalDateTime.now());
            airbnbSyncRepository.save(syncConfig.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sincronização manual concluída para: " + propertyId);
            response.put("propertyId", propertyId);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("lastSync", syncConfig.get().getLastSync());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro na sincronização: " + e.getMessage()));
        }
    }

    /**
     * Sincronização manual para todas as conexões ativas
     */
    @PostMapping("/sync-now/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        try {
            List<AirbnbSync> activeConnections = airbnbSyncRepository.findByIsActiveTrue();
            
            if (activeConnections.isEmpty()) {
                return ResponseEntity.ok(createErrorResponse("Nenhuma conexão ativa encontrada."));
            }

            int successCount = 0;
            int errorCount = 0;
            List<Map<String, Object>> results = new ArrayList<>();

            for (AirbnbSync connection : activeConnections) {
                Map<String, Object> result = new HashMap<>();
                result.put("propertyId", connection.getPropertyId());
                result.put("propertyName", connection.getPropertyName());
                
                try {
                    airbnbICalService.syncAirbnbReservations(
                        connection.getIcalUrl(), 
                        connection.getPropertyId()
                    );
                    
                    connection.setLastSync(LocalDateTime.now());
                    airbnbSyncRepository.save(connection);
                    
                    result.put("status", "SUCCESS");
                    result.put("message", "Sincronizado com sucesso");
                    successCount++;
                    
                } catch (Exception e) {
                    result.put("status", "ERROR");
                    result.put("message", e.getMessage());
                    errorCount++;
                }
                
                results.add(result);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sincronização completa");
            response.put("totalConnections", activeConnections.size());
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("results", results);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro na sincronização geral: " + e.getMessage()));
        }
    }

    /**
     * Desativar conexão
     */
    @PutMapping("/connections/{propertyId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateConnection(@PathVariable String propertyId) {
        try {
            Optional<AirbnbSync> connection = airbnbSyncRepository.findByPropertyId(propertyId);
            
            if (connection.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Conexão não encontrada para: " + propertyId));
            }

            connection.get().setIsActive(false);
            airbnbSyncRepository.save(connection.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conexão desativada com sucesso");
            response.put("propertyId", propertyId);
            response.put("isActive", false);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro ao desativar conexão: " + e.getMessage()));
        }
    }

    /**
     * Ativar conexão
     */
    @PutMapping("/connections/{propertyId}/activate")
    public ResponseEntity<Map<String, Object>> activateConnection(@PathVariable String propertyId) {
        try {
            Optional<AirbnbSync> connection = airbnbSyncRepository.findByPropertyId(propertyId);
            
            if (connection.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Conexão não encontrada para: " + propertyId));
            }

            connection.get().setIsActive(true);
            connection.get().setLastSync(LocalDateTime.now());
            airbnbSyncRepository.save(connection.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conexão ativada com sucesso");
            response.put("propertyId", propertyId);
            response.put("isActive", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro ao ativar conexão: " + e.getMessage()));
        }
    }

    /**
     * Excluir conexão
     */
    @DeleteMapping("/connections/{propertyId}")
    public ResponseEntity<Map<String, Object>> deleteConnection(@PathVariable String propertyId) {
        try {
            Optional<AirbnbSync> connection = airbnbSyncRepository.findByPropertyId(propertyId);
            
            if (connection.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Conexão não encontrada para: " + propertyId));
            }

            airbnbSyncRepository.delete(connection.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conexão excluída com sucesso");
            response.put("propertyId", propertyId);
            response.put("deletedAt", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro ao excluir conexão: " + e.getMessage()));
        }
    }

    // Métodos auxiliares
    private boolean isValidAirbnbICalUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return url.contains("airbnb.com/calendar/ical/") && 
               (url.contains(".ics") || url.contains("ical/"));
    }

    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    private Map<String, Object> createSuccessResponse(String message, 
                                                     AirbnbBidirectionalRequest request,
                                                     String exportUrl,
                                                     AirbnbSync config) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("airbnbImportUrl", request.getAirbnbIcalUrl());
        response.put("yourExportUrl", exportUrl);
        response.put("calendarName", request.getCalendarName());
        response.put("propertyId", request.getPropertyId());
        response.put("propertyName", request.getPropertyName());
        response.put("configId", config.getId());
        response.put("nextSteps", getNextSteps(exportUrl, request.getCalendarName()));
        response.put("setupDate", LocalDateTime.now().toString());
        response.put("syncDirection", config.getSyncDirection().toString());
        
        return response;
    }

    private List<String> getNextSteps(String exportUrl, String calendarName) {
        return Arrays.asList(
            "1. No Airbnb, vá em 'Calendário' → 'Importar calendário'",
            "2. Cole esta URL no campo 'Link para outro site': " + exportUrl,
            "3. No campo 'Nome do calendário', use: " + calendarName,
            "4. Clique em 'Salvar'",
            "5. Aguarde a sincronização automática (pode levar alguns minutos)",
            "6. As reservas serão sincronizadas nos dois sentidos automaticamente!"
        );
    }

    private List<String> getCalendarNextSteps(String exportUrl, String calendarName) {
        return Arrays.asList(
            "1. No Airbnb, vá em 'Calendário' → 'Importar calendário'",
            "2. Cole esta URL no campo 'Link para outro site': " + exportUrl,
            "3. No campo 'Nome do calendário', use: " + calendarName,
            "4. Clique em 'Salvar'",
            "5. Todas as reservas do seu calendário serão sincronizadas automaticamente",
            "6. Inclui: Reservas Confirmadas, Check-in Realizado, Check-out Realizado",
            "7. Exclui: Reservas Canceladas"
        );
    }
}