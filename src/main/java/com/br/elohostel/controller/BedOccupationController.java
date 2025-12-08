package com.br.elohostel.controller;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.BedAvailabilityInfo;
import com.br.elohostel.service.BedOccupationService;

@RestController
@RequestMapping("/api/bed-occupations")
public class BedOccupationController {

    private final BedOccupationService bedOccupationService;

    public BedOccupationController(BedOccupationService bedOccupationService) {
        this.bedOccupationService = bedOccupationService;
    }

    @GetMapping("/availability")
    public ResponseEntity<List<BedAvailabilityInfo>> getBedAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Set<LocalDate> dates = generateDateRange(startDate, endDate);
        List<BedAvailabilityInfo> availability = bedOccupationService
            .getBedAvailabilityByRoomAndDates(dates);
        
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getOccupationStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> statistics = bedOccupationService
            .getBedOccupationStatistics(startDate, endDate);
        
        return ResponseEntity.ok(statistics);
    }

    @PostMapping("/{reserveId}/allocate")
    public ResponseEntity<String> allocateBedsForReserve(@PathVariable Long reserveId) {
        // Lógica para alocar camas para uma reserva específica
        // (você precisará injetar o ReserveRepository também)
        return ResponseEntity.ok("Camas alocadas com sucesso");
    }

    private Set<LocalDate> generateDateRange(LocalDate start, LocalDate end) {
        Set<LocalDate> dates = new HashSet<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }
}