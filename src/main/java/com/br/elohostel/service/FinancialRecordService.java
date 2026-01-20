package com.br.elohostel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.br.elohostel.model.FinancialRecord;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.RoomTypeRevenue;
import com.br.elohostel.model.enums.PeriodType;
import com.br.elohostel.model.enums.ReserveStatus;
import com.br.elohostel.model.enums.RoomType;
import com.br.elohostel.repository.FinancialRecordRepository;
import com.br.elohostel.repository.ReserveRepository;
import com.br.elohostel.repository.RoomRepository;

import jakarta.transaction.Transactional;

@Service
public class FinancialRecordService {
    private static final Logger logger = Logger.getLogger(FinancialRecordService.class.getName());

    private final FinancialRecordRepository financialRecordRepo;
    private final ReserveRepository reserveRepo;
    private final RoomRepository roomRepo;

    public FinancialRecordService(FinancialRecordRepository financialRecordRepo, 
                                 ReserveRepository reserveRepo,
                                 RoomRepository roomRepo) {
        this.financialRecordRepo = financialRecordRepo;
        this.reserveRepo = reserveRepo;
        this.roomRepo = roomRepo;
    }

    public ReserveRepository getReserveRepository() {
        return reserveRepo;
    }

    public FinancialRecordRepository getFinancialRecordRepository() {
        return financialRecordRepo;
    }

    public RoomRepository getRoomRepository() {
        return roomRepo;
    }

    @Transactional
    public void processFinancialRecords() {
        logger.info("🔄 Iniciando processamento de registros financeiros...");
        
        LocalDate today = LocalDate.now();
        processDailyRecord(today);
        if (today.getDayOfMonth() == 1) {
            processMonthlyRecord(today.minusMonths(1));
        }

        if (today.getMonthValue() == 1 && today.getDayOfMonth() == 1) {
            processAnnualRecord(today.getYear() - 1);
        }
        
        logger.info("✅ Processamento de registros financeiros concluído.");
    }

    @Transactional
    public FinancialRecord processDailyRecord(LocalDate date) {
        logger.info("📅 Processando registro diário para: " + date);
        FinancialRecord dailyRecord = financialRecordRepo
            .findByRecordDateAndPeriodType(date, PeriodType.DIARIO)
            .orElse(new FinancialRecord(date, PeriodType.DIARIO));
        List<Reserve> dailyReservations = findUnprocessedReservationsForDate(date);
        if (!dailyReservations.isEmpty()) {
            calculateMetrics(dailyRecord, dailyReservations, date, PeriodType.DIARIO);
            FinancialRecord savedRecord = financialRecordRepo.save(dailyRecord);
            logger.info("✅ Registro diário salvo: " + date + " - " + 
                       dailyReservations.size() + " reservas processadas - Receita: " + savedRecord.getTotalRevenue());
            return savedRecord;
        } else {
            logger.info("⏭️ Nenhuma reserva não processada para: " + date);
            return dailyRecord;
        }
    }

    @Transactional
    public FinancialRecord processMonthlyRecord(LocalDate anyDateInMonth) {
        YearMonth yearMonth = YearMonth.from(anyDateInMonth);
        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        FinancialRecord monthlyRecord = financialRecordRepo
            .findByRecordDateAndPeriodType(firstDayOfMonth, PeriodType.MENSAL)
            .orElse(new FinancialRecord(firstDayOfMonth, PeriodType.MENSAL));
        List<Reserve> monthlyReservations = findUnprocessedReservationsForMonth(
            yearMonth.getYear(), yearMonth.getMonthValue());
        
        if (!monthlyReservations.isEmpty()) {
            calculateMetrics(monthlyRecord, monthlyReservations, firstDayOfMonth, PeriodType.MENSAL);
            FinancialRecord savedRecord = financialRecordRepo.save(monthlyRecord);
            logger.info("✅ Registro mensal salvo: " + yearMonth + " - " + 
                       monthlyReservations.size() + " reservas processadas - Receita: " + savedRecord.getTotalRevenue());
            return savedRecord;
        } else {
            logger.info("⏭️ Nenhuma reserva não processada para o mês: " + yearMonth);
            return monthlyRecord;
        }
    }

    @Transactional
    public FinancialRecord processAnnualRecord(int year) {
        LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
        
        logger.info("📈 Processando registro anual para: " + year);
        FinancialRecord annualRecord = financialRecordRepo
            .findByRecordDateAndPeriodType(firstDayOfYear, PeriodType.ANUAL)
            .orElse(new FinancialRecord(firstDayOfYear, PeriodType.ANUAL));
        List<Reserve> annualReservations = findUnprocessedReservationsForYear(year);
        
        if (!annualReservations.isEmpty()) {
            calculateMetrics(annualRecord, annualReservations, firstDayOfYear, PeriodType.ANUAL);
            FinancialRecord savedRecord = financialRecordRepo.save(annualRecord);
            logger.info("✅ Registro anual salvo: " + year + " - " + 
                       annualReservations.size() + " reservas processadas - Receita: " + savedRecord.getTotalRevenue());
            return savedRecord;
        } else {
            logger.info("⏭️ Nenhuma reserva não processada para o ano: " + year);
            return annualRecord;
        }
    }

    private List<Reserve> findUnprocessedReservationsForDate(LocalDate date) {
        List<Reserve> allConfirmed = reserveRepo.findByReserveStatus(ReserveStatus.CONFIRMED);
        for (Reserve reserve : allConfirmed) {
            logger.info("📋 Reserva #" + reserve.getId() + 
                       " - Status: " + reserve.getReserveStatus() +
                       " - Processada: " + reserve.getFinancialProcessed() +
                       " - Dias: " + reserve.getReservedDays() +
                       " - Contém " + date + ": " + (reserve.getReservedDays() != null && reserve.getReservedDays().contains(date)));
        }

        List<Reserve> forDate = allConfirmed.stream()
            .filter(reserve -> reserve.getReservedDays() != null && reserve.getReservedDays().contains(date))
            .collect(Collectors.toList());

        List<Reserve> unprocessed = forDate.stream()
            .filter(reserve -> !Boolean.TRUE.equals(reserve.getFinancialProcessed()))
            .collect(Collectors.toList());
        
        return unprocessed;
    }

    private List<Reserve> findUnprocessedReservationsForMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        logger.info("🔍 Buscando reservas não processadas para o mês: " + year + "-" + month);
        
        List<Reserve> reservations = reserveRepo.findByReserveStatusAndReservedDaysBetween(ReserveStatus.CONFIRMED, startDate, endDate).stream()
            .filter(reserve -> !Boolean.TRUE.equals(reserve.getFinancialProcessed()))
            .collect(Collectors.toList());
            
        logger.info("📊 Encontradas " + reservations.size() + " reservas não processadas para o mês " + year + "-" + month);
        
        return reservations;
    }

    private List<Reserve> findUnprocessedReservationsForYear(int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        logger.info("🔍 Buscando reservas não processadas para o ano: " + year);
        
        List<Reserve> reservations = reserveRepo.findByReserveStatusAndReservedDaysBetween(ReserveStatus.CONFIRMED, startDate, endDate).stream()
            .filter(reserve -> !Boolean.TRUE.equals(reserve.getFinancialProcessed()))
            .collect(Collectors.toList());
            
        logger.info("📊 Encontradas " + reservations.size() + " reservas não processadas para o ano " + year);
        
        return reservations;
    }

    private BigDecimal calculateDailyRevenue(Reserve reservation, LocalDate date) {
        BigDecimal totalReservationValue = reservation.calculateTotalValue();
        int totalDays = reservation.getNumberOfDays();
        
        if (totalDays == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalReservationValue.divide(
            BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
    }

    private void calculateMetrics(FinancialRecord record, List<Reserve> reservations, 
                            LocalDate referenceDate, PeriodType periodType) {

        logger.info("🔍 Calculando métricas para " + periodType + " - " + referenceDate + 
                   " - " + reservations.size() + " reservas não processadas encontradas");
        if (record.getId() == null) {
            record.setTotalRevenue(BigDecimal.ZERO);
            record.setTotalReservations(0);
            record.setTotalGuests(0);
            record.setTotalNights(0);
            record.getRoomTypeRevenues().clear();
        }
        
        for (Reserve reservation : reservations) {
            BigDecimal dailyRevenue = calculateDailyRevenue(reservation, referenceDate);
            int roomCount = reservation.getRooms().size();
            record.addRevenue(dailyRevenue);
            record.incrementReservations();
            record.addGuests(reservation.getGuest().size());
            record.addNights(roomCount);

            if (!reservation.getRooms().isEmpty()) {
                if (roomCount == 1) {
                    Room room = reservation.getRooms().iterator().next();
                    addRoomTypeRevenue(record, room.getRoomType(), dailyRevenue, 1, 1);
                } else {
                    BigDecimal valuePerRoom = dailyRevenue.divide(
                        BigDecimal.valueOf(roomCount), 2, RoundingMode.HALF_UP);
                    
                    for (Room room : reservation.getRooms()) {
                        addRoomTypeRevenue(record, room.getRoomType(), valuePerRoom, 1, 1);
                    }
                }
            }
            
            reservation.setFinancialProcessed(true);
            reserveRepo.save(reservation);
            logger.info("✅ Reserva #" + reservation.getId() + " marcada como processada");
        }
        
        logger.info("📊 Métricas finais - Receita: " + record.getTotalRevenue() +
                   " - Reservas: " + record.getTotalReservations() +
                   " - Noites: " + record.getTotalNights() +
                   " - Hóspedes: " + record.getTotalGuests());
        
        calculateDerivedMetrics(record, referenceDate, periodType);
        record.setUpdatedAt(LocalDateTime.now());
    }

    private void addRoomTypeRevenue(FinancialRecord record, RoomType roomType, 
                                  BigDecimal revenue, int nights, int reservations) {
        
        RoomTypeRevenue roomTypeRevenue = record.getRoomTypeRevenues().stream()
            .filter(rtr -> rtr.getRoomType() == roomType)
            .findFirst()
            .orElse(new RoomTypeRevenue(roomType));
        
        roomTypeRevenue.addRevenue(revenue);
        roomTypeRevenue.addNights(nights);
        for (int i = 0; i < reservations; i++) {
            roomTypeRevenue.incrementReservations();
        }
        
        record.getRoomTypeRevenues().add(roomTypeRevenue);
    }

    private void calculateDerivedMetrics(FinancialRecord record, LocalDate referenceDate, PeriodType periodType) {
        if (record.getTotalNights() > 0) {
            BigDecimal adr = record.getTotalRevenue()
                .divide(BigDecimal.valueOf(record.getTotalNights()), 2, RoundingMode.HALF_UP);
            record.setAverageDailyRate(adr);
        }
        
        int totalRooms = roomRepo.findAll().size();
        int daysInPeriod = getDaysInPeriod(periodType, referenceDate);
        int availableRoomNights = totalRooms * daysInPeriod;
        
        if (availableRoomNights > 0) {
            BigDecimal occupancy = BigDecimal.valueOf(record.getTotalNights())
                .divide(BigDecimal.valueOf(availableRoomNights), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            record.setOccupancyRate(occupancy);
        }
        
        if (totalRooms > 0 && daysInPeriod > 0) {
            BigDecimal revPAR = record.getTotalRevenue()
                .divide(BigDecimal.valueOf(totalRooms * daysInPeriod), 2, RoundingMode.HALF_UP);
            record.setRevPAR(revPAR);
        }
    }

    private int getDaysInPeriod(PeriodType periodType, LocalDate referenceDate) {
        switch (periodType) {
            case DIARIO:
                return 1;
            case MENSAL:
                return YearMonth.from(referenceDate).lengthOfMonth();
            case ANUAL:
                return referenceDate.lengthOfYear();
            default:
                return 1;
        }
    }

    @Transactional
    public void reprocessReservations(LocalDate startDate, LocalDate endDate) {
        logger.info("🔄 Reprocessamento manual de reservas de " + startDate + " a " + endDate);

        List<Reserve> reservationsToReprocess = reserveRepo.findByReserveStatusAndProcessedAndDateRange(
            ReserveStatus.CONFIRMED, true, startDate, endDate);
        
        for (Reserve reservation : reservationsToReprocess) {
            reservation.setFinancialProcessed(false);
        }
        reserveRepo.saveAll(reservationsToReprocess);
        
        logger.info("✅ " + reservationsToReprocess.size() + " reservas marcadas para reprocessamento");
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            processDailyRecord(date);
        }
        
        logger.info("✅ Reprocessamento manual concluído");
    }

    @Transactional
    public void resetAllProcessedFlags() {
        logger.info("🔄 Resetando todas as flags de processamento...");
        
        List<Reserve> allReservations = reserveRepo.findByReserveStatus(ReserveStatus.CONFIRMED);
        for (Reserve reservation : allReservations) {
            reservation.setFinancialProcessed(false);
        }
        reserveRepo.saveAll(allReservations);
        
        logger.info("✅ " + allReservations.size() + " reservas marcadas como não processadas");
    }

    @Transactional
    public void markAllReservationsAsProcessed() {
        logger.info("📝 Marcando todas as reservas confirmadas como processadas...");
        
        int updatedCount = reserveRepo.markReservationsAsProcessedByStatus(ReserveStatus.CONFIRMED);
        
        logger.info("✅ " + updatedCount + " reservas confirmadas marcadas como processadas");
    }
    
    public List<FinancialRecord> getDailyRecords(LocalDate startDate, LocalDate endDate) {
        return financialRecordRepo.findByPeriodTypeAndRecordDateBetween(
            PeriodType.DIARIO, startDate, endDate);
    }
    
    public List<FinancialRecord> getMonthlyRecords(int year) {
        return financialRecordRepo.findByPeriodTypeAndYear(PeriodType.MENSAL, year);
    }
    
    public List<FinancialRecord> getAnnualRecords() {
        return financialRecordRepo.findByPeriodTypeOrderByRecordDateDesc(PeriodType.ANUAL);
    }

    public Map<String, Object> getFinancialDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        
        Map<String, Object> dashboard = new HashMap<>();
        FinancialRecord todayRecord = financialRecordRepo
            .findByRecordDateAndPeriodType(today, PeriodType.DIARIO)
            .orElse(new FinancialRecord(today, PeriodType.DIARIO));
        
        dashboard.put("dailyRevenue", todayRecord.getTotalRevenue() != null ? todayRecord.getTotalRevenue() : BigDecimal.ZERO);
        dashboard.put("monthlyRevenue", financialRecordRepo
            .sumTotalRevenueByPeriodTypeAndDateRange(PeriodType.DIARIO, firstDayOfMonth, today) != null ? 
            financialRecordRepo.sumTotalRevenueByPeriodTypeAndDateRange(PeriodType.DIARIO, firstDayOfMonth, today) : BigDecimal.ZERO);
        dashboard.put("annualRevenue", financialRecordRepo
            .sumTotalRevenueByPeriodTypeAndDateRange(PeriodType.DIARIO, firstDayOfYear, today) != null ? 
            financialRecordRepo.sumTotalRevenueByPeriodTypeAndDateRange(PeriodType.DIARIO, firstDayOfYear, today) : BigDecimal.ZERO);
        
        BigDecimal todayForecast = calculateTodayForecast();
        dashboard.put("todayForecast", todayForecast);
        dashboard.put("hasActualData", todayRecord.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0);
        
        dashboard.put("occupancyRate", todayRecord.getOccupancyRate() != null ? 
            todayRecord.getOccupancyRate().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        dashboard.put("averageDailyRate", todayRecord.getAverageDailyRate() != null ? 
            todayRecord.getAverageDailyRate() : BigDecimal.ZERO);
        dashboard.put("revPAR", todayRecord.getRevPAR() != null ? 
            todayRecord.getRevPAR() : BigDecimal.ZERO);
        dashboard.put("totalNights", todayRecord.getTotalNights() != null ? 
            todayRecord.getTotalNights() : 0);
        
        long todayReservations = findUnprocessedReservationsForDate(today).size();
        dashboard.put("todayReservations", todayReservations);
        long occupiedRooms = roomRepo.findAll().stream()
            .filter(room -> room.getRoomStatus().name().equals("OCCUPIED"))
            .count();
        dashboard.put("occupiedRooms", occupiedRooms);
        return dashboard;
    }

    private BigDecimal calculateTodayForecast() {
        LocalDate today = LocalDate.now();
        List<Reserve> allConfirmed = reserveRepo.findByReserveStatus(ReserveStatus.CONFIRMED);

        List<Reserve> upcomingReservations = allConfirmed.stream()
            .filter(reserve -> reserve.getReservedDays() != null && !reserve.getReservedDays().isEmpty())
            .filter(reserve -> {
                LocalDate firstDay = reserve.getReservedDays().stream()
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.MAX);
                return !firstDay.isBefore(today);
            })
            .collect(Collectors.toList());
        
        BigDecimal totalForecast = BigDecimal.ZERO;
        for (Reserve reservation : upcomingReservations) {
            totalForecast = totalForecast.add(reservation.calculateTotalValue());
        }
        
        logger.info("🔮 Previsão total de reservas futuras: " + totalForecast);
        return totalForecast;
    }

    public void delete(Long id) {
        financialRecordRepo.deleteById(id);
    }

    @Transactional
    public FinancialRecord processSpecificDate(LocalDate date) {
        return processDailyRecord(date);
    }

    @Transactional
    public FinancialRecord forceProcessDate(LocalDate date) {
        List<Reserve> allConfirmed = reserveRepo.findByReserveStatus(ReserveStatus.CONFIRMED);
        List<Reserve> reservations = allConfirmed.stream()
            .filter(reserve -> reserve.getReservedDays() != null && reserve.getReservedDays().contains(date))
            .collect(Collectors.toList());
        FinancialRecord record = financialRecordRepo
            .findByRecordDateAndPeriodType(date, PeriodType.DIARIO)
            .orElse(new FinancialRecord(date, PeriodType.DIARIO));
        
        if (!reservations.isEmpty()) {
            record.setTotalRevenue(BigDecimal.ZERO);
            record.setTotalReservations(0);
            record.setTotalGuests(0);
            record.setTotalNights(0);
            record.getRoomTypeRevenues().clear();
            
            calculateMetricsForce(record, reservations, date, PeriodType.DIARIO);
            FinancialRecord savedRecord = financialRecordRepo.save(record);
            logger.info("✅ Registro forçado salvo: " + date + " - " + 
                       reservations.size() + " reservas - Receita: " + savedRecord.getTotalRevenue());
            return savedRecord;
        } else {
            logger.info("⏭️ Nenhuma reserva encontrada para: " + date);
            return record;
        }
    }

    private void calculateMetricsForce(FinancialRecord record, List<Reserve> reservations, 
                                LocalDate referenceDate, PeriodType periodType) {

        for (Reserve reservation : reservations) {
            BigDecimal dailyRevenue = calculateDailyRevenue(reservation, referenceDate);
            int roomCount = reservation.getRooms().size();
            
            record.addRevenue(dailyRevenue);
            record.incrementReservations();
            record.addGuests(reservation.getGuest().size());
            record.addNights(roomCount);

            if (!reservation.getRooms().isEmpty()) {
                if (roomCount == 1) {
                    Room room = reservation.getRooms().iterator().next();
                    addRoomTypeRevenue(record, room.getRoomType(), dailyRevenue, 1, 1);
                } else {
                    BigDecimal valuePerRoom = dailyRevenue.divide(
                        BigDecimal.valueOf(roomCount), 2, RoundingMode.HALF_UP);
                    
                    for (Room room : reservation.getRooms()) {
                        addRoomTypeRevenue(record, room.getRoomType(), valuePerRoom, 1, 1);
                    }
                }
            }
        }
        
        calculateDerivedMetrics(record, referenceDate, periodType);
        record.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void debugReservations() {
        List<Reserve> allReservations = reserveRepo.findAll();
        logger.info("📊 Total de reservas: " + allReservations.size());
        
        for (Reserve reserve : allReservations) {
            logger.info("🔍 Reserva #" + reserve.getId() + 
                       " - Status: " + reserve.getReserveStatus() +
                       " - Processada: " + reserve.getFinancialProcessed() +
                       " - Dias: " + reserve.getReservedDays() +
                       " - Hóspedes: " + reserve.getGuest().size() +
                       " - Quartos: " + reserve.getRooms().size() +
                       " - Valor Total: " + reserve.calculateTotalValue());
        }
        
        LocalDate today = LocalDate.now();
        List<Reserve> todayReservations = reserveRepo.findByReserveStatus(ReserveStatus.CONFIRMED).stream()
            .filter(reserve -> reserve.getReservedDays() != null && reserve.getReservedDays().contains(today))
            .collect(Collectors.toList());
        
        logger.info("📅 Reservas confirmadas para HOJE (" + today + "): " + todayReservations.size());
        
        for (Reserve reserve : todayReservations) {
            logger.info("✅ Reserva HOJE #" + reserve.getId() + 
                       " - Processada: " + reserve.getFinancialProcessed() +
                       " - Valor: " + reserve.calculateTotalValue());
        }
    }

    @Transactional
    public FinancialRecord processDate(LocalDate date, boolean force) {
        if (force) {
            return forceProcessDate(date);
        } else {
            return processDailyRecord(date);
        }
    }

    public Map<String, Object> checkReservationStatus(Long reservationId) {
        Reserve reservation = reserveRepo.findById(reservationId)
            .orElseThrow(() -> new RuntimeException("Reserva não encontrada: " + reservationId));
        
        Map<String, Object> status = new HashMap<>();
        status.put("id", reservation.getId());
        status.put("status", reservation.getReserveStatus());
        status.put("financialProcessed", reservation.getFinancialProcessed());
        status.put("reservedDays", reservation.getReservedDays());
        status.put("totalValue", reservation.calculateTotalValue());
        status.put("numberOfDays", reservation.getNumberOfDays());
        status.put("numberOfGuests", reservation.getGuest().size());
        status.put("numberOfRooms", reservation.getRooms().size());
        
        List<FinancialRecord> records = new ArrayList<>();
        for (LocalDate date : reservation.getReservedDays()) {
            financialRecordRepo.findByRecordDateAndPeriodType(date, PeriodType.DIARIO)
                .ifPresent(records::add);
        }
        status.put("financialRecords", records);
        
        return status;
    }

    @Transactional
    public FinancialRecord processForecast(LocalDate date) {
        logger.info("🔮 Processando previsão para: " + date);
        
        FinancialRecord forecastRecord = financialRecordRepo
            .findByRecordDateAndPeriodType(date, PeriodType.DIARIO)
            .orElse(new FinancialRecord(date, PeriodType.DIARIO));
        
        List<Reserve> allConfirmed = reserveRepo.findByReserveStatus(ReserveStatus.CONFIRMED);
        List<Reserve> reservationsForDate = allConfirmed.stream()
            .filter(reserve -> reserve.getReservedDays() != null && reserve.getReservedDays().contains(date))
            .collect(Collectors.toList());
        
        logger.info("📊 Previsão - " + reservationsForDate.size() + " reservas para " + date);
        
        if (!reservationsForDate.isEmpty()) {
            calculateMetricsForce(forecastRecord, reservationsForDate, date, PeriodType.DIARIO);
            FinancialRecord savedRecord = financialRecordRepo.save(forecastRecord);
            logger.info("✅ Previsão salva: " + date + " - Receita prevista: " + savedRecord.getTotalRevenue());
            return savedRecord;
        } else {
            logger.info("⏭️ Nenhuma reserva para previsão em: " + date);
            return forecastRecord;
        }
    }

    @Transactional
    public List<FinancialRecord> processForecastPeriod(LocalDate startDate, LocalDate endDate) {
        logger.info("🔮 Processando previsão para período: " + startDate + " a " + endDate);
        
        List<FinancialRecord> records = new ArrayList<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            FinancialRecord record = processForecast(date);
            records.add(record);
        }
        
        logger.info("✅ Previsão concluída - " + records.size() + " dias processados");
        return records;
    }

    @Transactional
    public String processAllReservations() {
        logger.info("🔄 Processando TODAS as reservas existentes...");
        
        LocalDate today = LocalDate.now();
        List<Reserve> allReservations = reserveRepo.findByReserveStatus(ReserveStatus.CONFIRMED);
        Set<LocalDate> allReservedDates = allReservations.stream()
            .filter(reserve -> reserve.getReservedDays() != null)
            .flatMap(reserve -> reserve.getReservedDays().stream())
            .collect(Collectors.toSet());
        
        logger.info("📊 Encontradas " + allReservedDates.size() + " datas únicas com reservas");
        
        int processedCount = 0;
        for (LocalDate date : allReservedDates) {
            forceProcessDate(date);
            processedCount++;
        }
        
        String result = "Processadas " + processedCount + " datas com reservas";
        logger.info("✅ " + result);
        return result;
    }
}