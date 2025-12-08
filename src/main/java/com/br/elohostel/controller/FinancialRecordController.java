package com.br.elohostel.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.FinancialRecord;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.service.FinancialRecordService;

@RestController
@RequestMapping("/financial")
public class FinancialRecordController {

    private final FinancialRecordService financialRecordService;

    public FinancialRecordController(FinancialRecordService financialRecordService) {
        this.financialRecordService = financialRecordService;
    }

    // ========== PROCESSAMENTO AUTOMÁTICO ==========
    
    @PostMapping("/process")
    public ResponseEntity<String> processFinancialRecords() {
        try {
            financialRecordService.processFinancialRecords();
            return ResponseEntity.ok("Registros financeiros processados com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Erro ao processar registros: " + e.getMessage());
        }
    }

    @PostMapping("/process/now")
    public ResponseEntity<String> processFinancialRecordsNow() {
        try {
            financialRecordService.processFinancialRecords();
            return ResponseEntity.ok("Registros financeiros processados com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Erro ao processar registros: " + e.getMessage());
        }
    }

    // ========== PROCESSAMENTO POR DATA ==========

    @PostMapping("/process/date")
    public ResponseEntity<String> processFinancialRecordsForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            financialRecordService.processDailyRecord(date);
            return ResponseEntity.ok("Registro diário processado para: " + date);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Erro ao processar registro: " + e.getMessage());
        }
    }

    @PostMapping("/process/date/{date}")
    public ResponseEntity<FinancialRecord> processDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean force) {
        try {
            FinancialRecord record = financialRecordService.processDate(date, force);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ✅ NOVO: Processamento forçado de uma data específica
    @PostMapping("/process/force/{date}")
    public ResponseEntity<FinancialRecord> forceProcessDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            FinancialRecord record = financialRecordService.forceProcessDate(date);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ========== PREVISÃO FINANCEIRA ==========

    // ✅ NOVO: Previsão financeira para uma data específica
    @PostMapping("/forecast/{date}")
    public ResponseEntity<FinancialRecord> processForecast(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            FinancialRecord record = financialRecordService.processForecast(date);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ✅ NOVO: Previsão financeira para um período
    @PostMapping("/forecast/period")
    public ResponseEntity<List<FinancialRecord>> processForecastPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<FinancialRecord> records = financialRecordService.processForecastPeriod(startDate, endDate);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ✅ NOVO: Processar todas as reservas existentes
    @PostMapping("/process/all-reservations")
    public ResponseEntity<String> processAllReservations() {
        try {
            String result = financialRecordService.processAllReservations();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Erro ao processar todas as reservas: " + e.getMessage());
        }
    }

    // ========== CONSULTAS E RELATÓRIOS ==========
    
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        try {
            return ResponseEntity.ok(financialRecordService.getFinancialDashboard());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/daily")
    public ResponseEntity<List<FinancialRecord>> getDailyRecords(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(financialRecordService.getDailyRecords(startDate, endDate));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<FinancialRecord>> getMonthlyRecords(@RequestParam int year) {
        try {
            return ResponseEntity.ok(financialRecordService.getMonthlyRecords(year));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/annual")
    public ResponseEntity<List<FinancialRecord>> getAnnualRecords() {
        try {
            return ResponseEntity.ok(financialRecordService.getAnnualRecords());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ========== DEBUG E DIAGNÓSTICO ==========
    
    // ✅ NOVO: Endpoint para debug das reservas
    @GetMapping("/debug/reservations")
    public ResponseEntity<String> debugReservations() {
        try {
            financialRecordService.debugReservations();
            return ResponseEntity.ok("Debug executado - verifique os logs");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Erro no debug: " + e.getMessage());
        }
    }

    // ✅ NOVO: Verificar status de uma reserva específica
    @GetMapping("/debug/reservation/{id}")
    public ResponseEntity<Map<String, Object>> checkReservationStatus(@PathVariable Long id) {
        try {
            Map<String, Object> status = financialRecordService.checkReservationStatus(id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ✅ NOVO: Debug detalhado de uma reserva específica
    @GetMapping("/debug/reservation/{id}/details")
    public ResponseEntity<Map<String, Object>> getReservationDetails(@PathVariable Long id) {
        try {
            Map<String, Object> status = financialRecordService.checkReservationStatus(id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ========== MANUTENÇÃO E ADMINISTRAÇÃO ==========

    // ✅ NOVO: Resetar flags de processamento (apenas desenvolvimento)
    @PostMapping("/reset-flags")
    public ResponseEntity<String> resetAllProcessedFlags() {
        try {
            financialRecordService.resetAllProcessedFlags();
            return ResponseEntity.ok("Flags de processamento resetadas");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Erro ao resetar flags: " + e.getMessage());
        }
    }

    // ✅ NOVO: Marcar todas como processadas
    @PostMapping("/mark-all-processed")
    public ResponseEntity<String> markAllReservationsAsProcessed() {
        try {
            financialRecordService.markAllReservationsAsProcessed();
            return ResponseEntity.ok("Todas as reservas confirmadas marcadas como processadas");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Erro ao marcar reservas: " + e.getMessage());
        }
    }

    // ✅ NOVO: Reprocessar período específico
    @PostMapping("/reprocess")
    public ResponseEntity<String> reprocessReservations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            financialRecordService.reprocessReservations(startDate, endDate);
            return ResponseEntity.ok("Reprocessamento concluído para o período: " + startDate + " a " + endDate);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Erro no reprocessamento: " + e.getMessage());
        }
    }

    // ✅ NOVO: Processar mês específico
    @PostMapping("/process/month")
    public ResponseEntity<FinancialRecord> processMonthlyRecord(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anyDateInMonth) {
        try {
            FinancialRecord record = financialRecordService.processMonthlyRecord(anyDateInMonth);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ✅ NOVO: Processar ano específico
    @PostMapping("/process/year/{year}")
    public ResponseEntity<FinancialRecord> processAnnualRecord(@PathVariable int year) {
        try {
            FinancialRecord record = financialRecordService.processAnnualRecord(year);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ========== OPERAÇÕES CRUD ==========

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            financialRecordService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // ✅ NOVO: Health check do serviço financeiro
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new java.util.HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", java.time.LocalDateTime.now());
            health.put("service", "FinancialRecordService");
            
            // Estatísticas básicas
            List<Reserve> allReservations = financialRecordService.getReserveRepository().findAll();
            List<FinancialRecord> allRecords = financialRecordService.getFinancialRecordRepository().findAll();
            
            health.put("totalReservations", allReservations.size());
            health.put("totalFinancialRecords", allRecords.size());
            health.put("confirmedReservations", allReservations.stream()
                .filter(r -> r.getReserveStatus() == com.br.elohostel.model.enums.ReserveStatus.CONFIRMED)
                .count());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }

    // ✅ NOVO: Estatísticas do sistema
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new java.util.HashMap<>();
            
            // Reservas
            List<Reserve> allReservations = financialRecordService.getReserveRepository().findAll();
            long confirmedReservations = allReservations.stream()
                .filter(r -> r.getReserveStatus() == com.br.elohostel.model.enums.ReserveStatus.CONFIRMED)
                .count();
            long processedReservations = allReservations.stream()
                .filter(r -> Boolean.TRUE.equals(r.getFinancialProcessed()))
                .count();
            
            stats.put("totalReservations", allReservations.size());
            stats.put("confirmedReservations", confirmedReservations);
            stats.put("processedReservations", processedReservations);
            stats.put("unprocessedReservations", confirmedReservations - processedReservations);
            
            // Registros financeiros
            List<FinancialRecord> allRecords = financialRecordService.getFinancialRecordRepository().findAll();
            long dailyRecords = allRecords.stream()
                .filter(r -> r.getPeriodType() == com.br.elohostel.model.enums.PeriodType.DIARIO)
                .count();
            long monthlyRecords = allRecords.stream()
                .filter(r -> r.getPeriodType() == com.br.elohostel.model.enums.PeriodType.MENSAL)
                .count();
            long annualRecords = allRecords.stream()
                .filter(r -> r.getPeriodType() == com.br.elohostel.model.enums.PeriodType.ANUAL)
                .count();
            
            stats.put("totalFinancialRecords", allRecords.size());
            stats.put("dailyRecords", dailyRecords);
            stats.put("monthlyRecords", monthlyRecords);
            stats.put("annualRecords", annualRecords);
            
            // Receita total
            BigDecimal totalRevenue = allRecords.stream()
                .map(FinancialRecord::getTotalRevenue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("totalRevenue", totalRevenue);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}

