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
import com.br.elohostel.util.TenantContext;

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
    
    @GetMapping("/export/{propertyId}/{token}.ics")
    public ResponseEntity<String> exportICalendar(@PathVariable String propertyId,
                                                @PathVariable String token) {
        try {
            Optional<AirbnbSync> syncConfig = airbnbSyncRepository.findAll().stream()
                .filter(s -> s.getPropertyId().equals(propertyId))
                .findFirst();
            
            if (syncConfig.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TenantContext.setCurrentTenant(syncConfig.get().getTenant().getTenantKey());
            
            try {
                String icalContent = icalExportService.generateICalContent(propertyId);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                    .header("Content-Disposition", "inline; filename=calendar_" + propertyId + ".ics")
                    .body(icalContent);
            } finally {
                TenantContext.clear();
            }
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("ERROR: " + e.getMessage());
        }
    }
    
    @GetMapping("/calendar/export/{propertyId}/{token}.ics")
    public ResponseEntity<String> exportCalendar(@PathVariable String propertyId,
                                               @PathVariable String token) {
        return exportICalendar(propertyId, token);
    }
}