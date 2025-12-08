package com.br.elohostel.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.AirbnbSync;
import com.br.elohostel.repository.AirbnbSyncRepository;
import com.br.elohostel.service.ICalExportService;

@RestController
@RequestMapping("/api/ical")
public class ICalExportController {
    
    private final ICalExportService icalExportService;
    private final AirbnbSyncRepository airbnbSyncRepository;
    
    public ICalExportController(ICalExportService icalExportService,
                              AirbnbSyncRepository airbnbSyncRepository) {
        this.icalExportService = icalExportService;
        this.airbnbSyncRepository = airbnbSyncRepository;
    }
    
    /**
     * Endpoint para exportação iCal (usado pelo Airbnb para importar)
     */
    @GetMapping("/export/{propertyId}/{token}.ics")
    public ResponseEntity<String> exportICalendar(@PathVariable String propertyId,
                                                @PathVariable String token) {
        try {
            // Validar se a configuração existe
            Optional<AirbnbSync> syncConfig = airbnbSyncRepository.findByPropertyId(propertyId);
            if (syncConfig.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Gerar conteúdo iCal
            String icalContent = icalExportService.generateICalContent(propertyId);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .header("Content-Disposition", "inline; filename=calendar_" + propertyId + ".ics")
                .body(icalContent);
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("ERROR: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint alternativo para o frontend
     */
    @GetMapping("/calendar/export/{propertyId}/{token}.ics")
    public ResponseEntity<String> exportCalendar(@PathVariable String propertyId,
                                               @PathVariable String token) {
        return exportICalendar(propertyId, token);
    }
}
