package com.br.elohostel.controller;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.br.elohostel.model.BookingSync;
import com.br.elohostel.model.dtos.BookingBidirectionalRequest;
import com.br.elohostel.repository.BookingSyncRepository;
import com.br.elohostel.service.BookingICalService;
import com.br.elohostel.service.ICalExportService;

@RestController
@RequestMapping("/api/booking")
public class BookingBidirectionalController {

    // @Value("${app.base-url:http://187.53.174.140:8080}")
    @Value("${app.base-url:http://[2804:d59:8501:eb00:e897:5209:e31b:8472]:8080}")
    private String baseUrl;

    private final BookingICalService bookingICalService;
    private final BookingSyncRepository bookingSyncRepository;
    private final ICalExportService icalExportService;

    public BookingBidirectionalController(BookingICalService bookingICalService,
                                       BookingSyncRepository bookingSyncRepository,
                                       ICalExportService icalExportService) {
        this.bookingICalService = bookingICalService;
        this.bookingSyncRepository = bookingSyncRepository;
        this.icalExportService = icalExportService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Booking.com Bidirectional Sync");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("baseUrl", baseUrl);
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/setup-bidirectional")
    public ResponseEntity<Map<String, Object>> setupBidirectional(
            @RequestBody BookingBidirectionalRequest request) {
        
        try {
            if (!isValidBookingICalUrl(request.getBookingIcalUrl())) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("URL iCal da Booking inválida. Deve ser uma URL válida do Booking.com calendar."));
            }

            Optional<BookingSync> existingConfig = bookingSyncRepository.findByPropertyId(request.getPropertyId());
            if (existingConfig.isPresent()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Já existe uma configuração para o Property ID: " + request.getPropertyId()));
            }

            String exportUrl = icalExportService.generateExportUrl(request.getPropertyId());
            String fullExportUrl = baseUrl + exportUrl;

            BookingSync syncConfig = new BookingSync();
            syncConfig.setIcalUrl(request.getBookingIcalUrl());
            syncConfig.setExportIcalUrl(fullExportUrl);
            syncConfig.setPropertyId(request.getPropertyId());
            syncConfig.setPropertyName(request.getPropertyName());
            syncConfig.setCalendarName(request.getCalendarName());
            syncConfig.setRoomNumber(request.getRoomNumber());
            syncConfig.setSyncDirection(BookingSync.SyncDirection.BIDIRECTIONAL);
            syncConfig.setIsActive(true);
            syncConfig.setLastSync(LocalDateTime.now());

            BookingSync savedConfig = bookingSyncRepository.save(syncConfig);

            CompletableFuture.runAsync(() -> {
                try {
                    bookingICalService.syncBookingReservations(request.getBookingIcalUrl(), request.getPropertyId());
                    System.out.println("Sincronização inicial da Booking concluída para: " + request.getPropertyId());
                } catch (Exception e) {
                    System.err.println("Erro na sincronização inicial da Booking para " + request.getPropertyId() + ": " + e.getMessage());
                }
            });

            Map<String, Object> response = createSuccessResponse(
                "Configuração bidirecional com Booking.com concluída com sucesso!",
                request,
                fullExportUrl,
                savedConfig
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro na configuração: " + e.getMessage()));
        }
    }

    @GetMapping("/connections")
    public ResponseEntity<List<BookingSync>> getConnections() {
        List<BookingSync> connections = bookingSyncRepository.findByIsActiveTrue();
        return ResponseEntity.ok(connections);
    }

    @GetMapping("/connections/{propertyId}")
    public ResponseEntity<Map<String, Object>> getConnection(@PathVariable String propertyId) {
        Optional<BookingSync> connection = bookingSyncRepository.findByPropertyId(propertyId);
        
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

    @PostMapping("/sync-now/{propertyId}")
    public ResponseEntity<Map<String, Object>> syncNow(@PathVariable String propertyId) {
        try {
            Optional<BookingSync> syncConfig = bookingSyncRepository.findByPropertyId(propertyId);
            if (syncConfig.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Configuração não encontrada para: " + propertyId));
            }

            bookingICalService.syncBookingReservations(
                syncConfig.get().getIcalUrl(), 
                propertyId
            );

            syncConfig.get().setLastSync(LocalDateTime.now());
            bookingSyncRepository.save(syncConfig.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sincronização manual da Booking concluída para: " + propertyId);
            response.put("propertyId", propertyId);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("lastSync", syncConfig.get().getLastSync());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro na sincronização: " + e.getMessage()));
        }
    }

    // MÉTODOS QUE FALTAVAM:

    @PostMapping("/sync-now/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        try {
            List<BookingSync> activeConnections = bookingSyncRepository.findByIsActiveTrue();
            
            if (activeConnections.isEmpty()) {
                return ResponseEntity.ok(createErrorResponse("Nenhuma conexão ativa da Booking encontrada."));
            }

            int successCount = 0;
            int errorCount = 0;
            List<Map<String, Object>> results = new ArrayList<>();

            for (BookingSync connection : activeConnections) {
                Map<String, Object> result = new HashMap<>();
                result.put("propertyId", connection.getPropertyId());
                result.put("propertyName", connection.getPropertyName());
                
                try {
                    bookingICalService.syncBookingReservations(
                        connection.getIcalUrl(), 
                        connection.getPropertyId()
                    );
                    
                    connection.setLastSync(LocalDateTime.now());
                    bookingSyncRepository.save(connection);
                    
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
            response.put("message", "Sincronização completa da Booking");
            response.put("totalConnections", activeConnections.size());
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("results", results);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro na sincronização geral da Booking: " + e.getMessage()));
        }
    }

    @PutMapping("/connections/{propertyId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateConnection(@PathVariable String propertyId) {
        try {
            Optional<BookingSync> connection = bookingSyncRepository.findByPropertyId(propertyId);
            
            if (connection.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Conexão da Booking não encontrada para: " + propertyId));
            }

            connection.get().setIsActive(false);
            bookingSyncRepository.save(connection.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conexão da Booking desativada com sucesso");
            response.put("propertyId", propertyId);
            response.put("isActive", false);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro ao desativar conexão da Booking: " + e.getMessage()));
        }
    }

    @PutMapping("/connections/{propertyId}/activate")
    public ResponseEntity<Map<String, Object>> activateConnection(@PathVariable String propertyId) {
        try {
            Optional<BookingSync> connection = bookingSyncRepository.findByPropertyId(propertyId);
            
            if (connection.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Conexão da Booking não encontrada para: " + propertyId));
            }

            connection.get().setIsActive(true);
            connection.get().setLastSync(LocalDateTime.now());
            bookingSyncRepository.save(connection.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conexão da Booking ativada com sucesso");
            response.put("propertyId", propertyId);
            response.put("isActive", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro ao ativar conexão da Booking: " + e.getMessage()));
        }
    }

    @DeleteMapping("/connections/{propertyId}")
    public ResponseEntity<Map<String, Object>> deleteConnection(@PathVariable String propertyId) {
        try {
            Optional<BookingSync> connection = bookingSyncRepository.findByPropertyId(propertyId);
            
            if (connection.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Conexão da Booking não encontrada para: " + propertyId));
            }

            bookingSyncRepository.delete(connection.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conexão da Booking excluída com sucesso");
            response.put("propertyId", propertyId);
            response.put("deletedAt", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro ao excluir conexão da Booking: " + e.getMessage()));
        }
    }

    // Endpoint específico para configuração de calendário do frontend
    @PostMapping("/setup-calendar-bidirectional")
    public ResponseEntity<Map<String, Object>> setupCalendarBidirectional(
            @RequestBody BookingBidirectionalRequest request) {
        
        try {
            if (!isValidBookingICalUrl(request.getBookingIcalUrl())) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("URL iCal da Booking inválida."));
            }

            Optional<BookingSync> existingConfig = bookingSyncRepository.findByPropertyId(request.getPropertyId());
            if (existingConfig.isPresent()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Já existe uma configuração para o Property ID: " + request.getPropertyId()));
            }

            String exportUrl = icalExportService.generateCalendarExportUrl(request.getPropertyId());
            String fullExportUrl = baseUrl + exportUrl;

            BookingSync syncConfig = new BookingSync();
            syncConfig.setIcalUrl(request.getBookingIcalUrl());
            syncConfig.setExportIcalUrl(fullExportUrl);
            syncConfig.setPropertyId(request.getPropertyId());
            syncConfig.setPropertyName(request.getPropertyName());
            syncConfig.setCalendarName(request.getCalendarName());
            syncConfig.setRoomNumber(request.getRoomNumber());
            syncConfig.setSyncDirection(BookingSync.SyncDirection.BIDIRECTIONAL);
            syncConfig.setIsActive(true);
            syncConfig.setLastSync(LocalDateTime.now());

            BookingSync savedConfig = bookingSyncRepository.save(syncConfig);

            CompletableFuture.runAsync(() -> {
                try {
                    bookingICalService.syncBookingReservations(request.getBookingIcalUrl(), request.getPropertyId());
                    System.out.println("Sincronização inicial do calendário da Booking concluída para: " + request.getPropertyId());
                } catch (Exception e) {
                    System.err.println("Erro na sincronização inicial do calendário da Booking para " + request.getPropertyId() + ": " + e.getMessage());
                }
            });

            Map<String, Object> response = createSuccessResponse(
                "Calendário conectado com Booking.com com sucesso!",
                request,
                fullExportUrl,
                savedConfig
            );
            
            response.put("calendarType", "FRONTEND_CALENDAR");
            response.put("syncFrequency", "A cada 3 horas");
            response.put("nextSteps", getCalendarNextSteps(fullExportUrl, request.getCalendarName()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro na configuração do calendário: " + e.getMessage()));
        }
    }

    // Método para testar conexão com a URL da Booking
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        try {
            String icalUrl = request.get("icalUrl");
            
            if (!isValidBookingICalUrl(icalUrl)) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("URL iCal da Booking inválida."));
            }

            // Tentar buscar o conteúdo iCal para testar a conexão
            try {
                bookingICalService.syncBookingReservations(icalUrl, "test-connection");
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Conexão com Booking.com testada com sucesso!");
                response.put("icalUrl", icalUrl);
                response.put("timestamp", LocalDateTime.now().toString());
                
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Falha ao conectar com a URL da Booking: " + e.getMessage()));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Erro ao testar conexão: " + e.getMessage()));
        }
    }

    private boolean isValidBookingICalUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return url.contains("booking.com") && url.contains(".ics");
    }

    private Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    private Map<String, Object> createSuccessResponse(String message, 
                                                     BookingBidirectionalRequest request,
                                                     String exportUrl,
                                                     BookingSync config) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("bookingImportUrl", request.getBookingIcalUrl());
        response.put("yourExportUrl", exportUrl);
        response.put("calendarName", request.getCalendarName());
        response.put("propertyId", request.getPropertyId());
        response.put("propertyName", request.getPropertyName());
        response.put("roomNumber", request.getRoomNumber());
        response.put("configId", config.getId());
        response.put("nextSteps", getNextSteps(exportUrl, request.getCalendarName()));
        response.put("setupDate", LocalDateTime.now().toString());
        response.put("syncDirection", config.getSyncDirection().toString());
        
        return response;
    }

    private List<String> getNextSteps(String exportUrl, String calendarName) {
        return Arrays.asList(
            "1. No Booking.com, vá em 'Calendário' → 'Conexões de calendário'",
            "2. Cole esta URL no campo 'URL do calendário': " + exportUrl,
            "3. No campo 'Nome do calendário', use: " + calendarName,
            "4. Clique em 'Salvar' ou 'Conectar'",
            "5. Aguarde a sincronização automática (pode levar alguns minutos)",
            "6. As reservas serão sincronizadas nos dois sentidos automaticamente!"
        );
    }

    private List<String> getCalendarNextSteps(String exportUrl, String calendarName) {
        return Arrays.asList(
            "1. No Booking.com, vá em 'Calendário' → 'Conexões de calendário'",
            "2. Cole esta URL no campo 'URL do calendário': " + exportUrl,
            "3. No campo 'Nome do calendário', use: " + calendarName,
            "4. Clique em 'Salvar' ou 'Conectar'",
            "5. Todas as reservas do seu calendário serão sincronizadas automaticamente",
            "6. Inclui: Reservas Confirmadas, Check-in Realizado, Check-out Realizado",
            "7. Exclui: Reservas Canceladas"
        );
    }
}