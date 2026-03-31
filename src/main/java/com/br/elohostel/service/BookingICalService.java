package com.br.elohostel.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.br.elohostel.model.BookingReservation;
import com.br.elohostel.model.BookingSync;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.dtos.ReservesionRequest;
import com.br.elohostel.repository.BookingReservationRepository;
import com.br.elohostel.repository.BookingSyncRepository;

@Service
@Transactional
public class BookingICalService {
    
    private static final Logger logger = Logger.getLogger(BookingICalService.class.getName());
    
    private final BookingSyncRepository bookingSyncRepository;
    private final BookingReservationRepository bookingReservationRepository;
    private final ReserveService reserveService;
    private final RestTemplate restTemplate;
    
    public BookingICalService(BookingSyncRepository bookingSyncRepository,
                           BookingReservationRepository bookingReservationRepository,
                           ReserveService reserveService) {
        this.bookingSyncRepository = bookingSyncRepository;
        this.bookingReservationRepository = bookingReservationRepository;
        this.reserveService = reserveService;
        this.restTemplate = new RestTemplate();
    }

    private String fetchICalContent(String icalUrl) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(icalUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Falha ao buscar iCal da Booking. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao acessar URL iCal da Booking: " + e.getMessage(), e);
        }
    }

    private List<BookingReservation> parseICalContent(String icalContent, BookingSync syncConfig) {
        List<BookingReservation> reservations = new ArrayList<>();
        
        try {
            String[] events = icalContent.split("BEGIN:VEVENT");
            
            for (String event : events) {
                if (event.contains("END:VEVENT")) {
                    BookingReservation reservation = parseEvent(event, syncConfig);
                    if (reservation != null) {
                        reservations.add(reservation);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warning("Erro ao parsear iCal da Booking: " + e.getMessage());
        }
        
        return reservations;
    }

    @Transactional
    public void syncBookingReservations(String icalUrl, String propertyId) {
        try {
            logger.info("Iniciando sincronização da Booking para property: " + propertyId);
            
            Optional<BookingSync> syncConfig = bookingSyncRepository.findByPropertyId(propertyId);
            if (syncConfig.isEmpty()) {
                throw new RuntimeException("Configuração de sync não encontrada para property: " + propertyId);
            }
            
            String icalContent = fetchICalContent(icalUrl);
            List<BookingReservation> bookingReservations = parseICalContent(icalContent, syncConfig.get());
            
            logger.info("📥 Reservas detectadas no iCal da Booking: " + bookingReservations.size());

            int processedCount = 0;
            int errorCount = 0;

            for (BookingReservation bookingReservation : bookingReservations) {
                try {
                    processBookingReservation(bookingReservation, syncConfig.get());
                    processedCount++;
                    
                } catch (Exception e) {
                    errorCount++;
                    logger.warning("❌ Erro ao processar reserva da Booking " + bookingReservation.getBookingReservationId() + ": " + e.getMessage());
                }
            }
            
            syncConfig.get().setLastSync(LocalDateTime.now());
            bookingSyncRepository.save(syncConfig.get());
            
            logger.info("✅ Sincronização da Booking concluída. " + processedCount + " processadas, " + errorCount + " erros.");
            
        } catch (Exception e) {
            logger.severe("❌ Erro crítico na sincronização da Booking: " + e.getMessage());
            throw new RuntimeException("Falha na sincronização: " + e.getMessage(), e);
        }
    }

    private void processBookingReservation(BookingReservation bookingReservation, BookingSync syncConfig) {
        try {
            Optional<BookingReservation> existing = bookingReservationRepository
                .findByBookingReservationId(bookingReservation.getBookingReservationId());
            
            if (existing.isPresent()) {
                logger.info("⏭️ Reserva da Booking já processada: " + bookingReservation.getBookingReservationId());
                return;
            }
            
            bookingReservationRepository.save(bookingReservation);
            
            try {
                createInternalReserve(bookingReservation, syncConfig);
                bookingReservation.setIsProcessed(true);
                logger.info("✅ Reserva da Booking criada com sucesso: " + bookingReservation.getBookingReservationId());
            } catch (Exception e) {
                bookingReservation.setIsProcessed(false);
                logger.warning("⚠️ Reserva da Booking salva mas não processada: " + bookingReservation.getBookingReservationId() + " - " + e.getMessage());
            }
            
            bookingReservationRepository.save(bookingReservation);
            
        } catch (Exception e) {
            logger.warning("❌ Erro ao processar reserva da Booking " + bookingReservation.getBookingReservationId() + ": " + e.getMessage());
        }
    }
    
    private void createInternalReserve(BookingReservation bookingReservation, BookingSync syncConfig) {
        try {
            Set<LocalDate> dates = getDatesBetween(bookingReservation.getCheckIn(), bookingReservation.getCheckOut());
            
            Integer roomNumber = syncConfig.getRoomNumber() != null ? syncConfig.getRoomNumber() : 101;
            
            ReservesionRequest request = new ReservesionRequest(
                dates,
                bookingReservation.getGuestName(),
                roomNumber
            );
            
            Reserve reserve = reserveService.createReserve(request);
            bookingReservation.setReserve(reserve);
            
            logger.info("🏠 Reserva interna criada a partir da Booking: #" + reserve.getId() + " no quarto " + roomNumber);
            
        } catch (Exception e) {
            logger.warning("Erro ao criar reserva interna a partir da Booking: " + e.getMessage());
            throw e;
        }
    }

    private BookingReservation parseEvent(String eventContent, BookingSync syncConfig) {
        try {
            BookingReservation reservation = new BookingReservation();
            reservation.setBookingSync(syncConfig);
            
            String uid = extractProperty(eventContent, "UID:");
            if (uid != null) {
                reservation.setBookingReservationId(uid);
            }
            
            String dtStart = extractProperty(eventContent, "DTSTART;VALUE=DATE:");
            String dtEnd = extractProperty(eventContent, "DTEND;VALUE=DATE:");
            
            if (dtStart != null && dtEnd != null) {
                reservation.setCheckIn(parseICalDate(dtStart));
                reservation.setCheckOut(parseICalDate(dtEnd));
            }
            
            String summary = extractProperty(eventContent, "SUMMARY:");
            if (summary != null) {
                reservation.setGuestName(extractGuestNameFromSummary(summary));
            }
            
            if (!isRealReservation(summary)) {
                logger.info("🔒 Ignorando bloqueio manual da Booking: " + summary);
                return null;
            }
            
            String description = extractProperty(eventContent, "DESCRIPTION:");
            if (description != null) {
                reservation.setGuestEmail(extractEmailFromDescription(description));
            }
            
            reservation.setStatus("CONFIRMED");
            reservation.setLastModified(LocalDateTime.now());
            reservation.setIsProcessed(false);
            
            logger.info("✅ Reserva real da Booking detectada: " + reservation.getGuestName() + 
                       " - " + reservation.getCheckIn() + " a " + reservation.getCheckOut());
            
            return reservation;
            
        } catch (Exception e) {
            logger.warning("Erro ao parsear evento da Booking: " + e.getMessage());
            return null;
        }
    }

    private boolean isRealReservation(String summary) {
        if (summary == null) return false;
        
        String lowerSummary = summary.toLowerCase();
        
        if (lowerSummary.contains("blocked") || 
            lowerSummary.contains("unavailable") ||
            lowerSummary.contains("bloqueado") ||
            lowerSummary.contains("indisponível")) {
            return false;
        }
        
        return lowerSummary.contains("reservation") || 
               lowerSummary.contains("reserva") ||
               containsGuestName(lowerSummary);
    }
    
    private boolean containsGuestName(String summary) {
        return summary.matches(".*[A-Z][a-z]+ [A-Z][a-z]+.*") ||
               summary.split(" ").length >= 2;
    }
    
    private String extractProperty(String content, String property) {
        int startIndex = content.indexOf(property);
        if (startIndex != -1) {
            startIndex += property.length();
            int endIndex = content.indexOf("\r\n", startIndex);
            if (endIndex == -1) endIndex = content.indexOf("\n", startIndex);
            
            if (endIndex != -1) {
                return content.substring(startIndex, endIndex).trim();
            }
        }
        return null;
    }
    
    private LocalDate parseICalDate(String dateStr) {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    
    private String extractGuestNameFromSummary(String summary) {
        if (summary.toLowerCase().contains("reservation for")) {
            return summary.substring(summary.toLowerCase().indexOf("reservation for") + 15).trim();
        }
        return summary;
    }
    
    private String extractEmailFromDescription(String description) {
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = emailPattern.matcher(description);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    
    private Set<LocalDate> getDatesBetween(LocalDate start, LocalDate end) {
        Set<LocalDate> dates = new HashSet<>();
        LocalDate current = start;
        while (current.isBefore(end)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }
    
    @Scheduled(fixedRate = 10800000)
    public void scheduledSync() {
        logger.info("🔄 Executando sincronização automática da Booking...");
        
        List<BookingSync> activeSyncs = bookingSyncRepository.findByIsActiveTrue();
        logger.info("📊 Conexões ativas da Booking: " + activeSyncs.size());
        
        for (BookingSync sync : activeSyncs) {
            try {
                logger.info("🔍 Sincronizando Booking: " + sync.getPropertyName());
                syncBookingReservations(sync.getIcalUrl(), sync.getPropertyId());
                logger.info("✅ Concluído: " + sync.getPropertyId());
            } catch (Exception e) {
                logger.severe("❌ Erro na sync da Booking para " + sync.getPropertyId() + ": " + e.getMessage());
            }
        }
        
        logger.info("🏁 Sincronização automática da Booking finalizada");
    } 
}