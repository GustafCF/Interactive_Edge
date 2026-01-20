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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.web.client.RestTemplate;

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.AirbnbReservation;
import com.br.elohostel.model.AirbnbSync;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.dtos.ReservesionRequest;
import com.br.elohostel.repository.AirbnbReservationRepository;
import com.br.elohostel.repository.AirbnbSyncRepository;
import com.br.elohostel.repository.RoomRepository;

@Service
@Transactional
public class AirbnbICalService {
    
    private static final Logger logger = Logger.getLogger(AirbnbICalService.class.getName());
    
    private final AirbnbSyncRepository airbnbSyncRepository;
    private final AirbnbReservationRepository airbnbReservationRepository;
    private final ReserveService reserveService;
    private final RestTemplate restTemplate;
    private final RoomRepository roomRepository; 

    public AirbnbICalService(AirbnbSyncRepository airbnbSyncRepository,
                        AirbnbReservationRepository airbnbReservationRepository,
                        ReserveService reserveService,
                        RoomRepository roomRepository) { 
        this.airbnbSyncRepository = airbnbSyncRepository;
        this.airbnbReservationRepository = airbnbReservationRepository;
        this.reserveService = reserveService;
        this.restTemplate = new RestTemplate();
        this.roomRepository = roomRepository; 
    }

    private String fetchICalContent(String icalUrl) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(icalUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Falha ao buscar iCal. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao acessar URL iCal: " + e.getMessage(), e);
        }
    }


    private List<AirbnbReservation> parseICalContent(String icalContent, AirbnbSync syncConfig) {
        List<AirbnbReservation> reservations = new ArrayList<>();
        
        try {
            String[] events = icalContent.split("BEGIN:VEVENT");
            
            for (String event : events) {
                if (event.contains("END:VEVENT")) {
                    AirbnbReservation reservation = parseEvent(event, syncConfig);
                    if (reservation != null) {
                        reservations.add(reservation);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warning("Erro ao parsear iCal: " + e.getMessage());
        }
        
        return reservations;
    }

public void syncAirbnbReservations(String icalUrl, String propertyId) {
    try {
        logger.info("Iniciando sincronização do Airbnb para property: " + propertyId);
        
        Optional<AirbnbSync> syncConfig = airbnbSyncRepository.findByPropertyId(propertyId);
        if (syncConfig.isEmpty()) {
            throw new RuntimeException("Configuração de sync não encontrada para property: " + propertyId);
        }
        
        String icalContent = fetchICalContent(icalUrl);
        List<AirbnbReservation> airbnbReservations = parseICalContent(icalContent, syncConfig.get());
        
        logger.info("Reservas detectadas no iCal: " + airbnbReservations.size());

        int processedCount = 0;
        int errorCount = 0;

        for (AirbnbReservation airbnbReservation : airbnbReservations) {
            try {
                processSingleReservation(airbnbReservation, syncConfig.get());
                processedCount++;
                
            } catch (Exception e) {
                errorCount++;
                logger.warning("Erro ao processar reserva " + airbnbReservation.getAirbnbReservationId() + ": " + e.getMessage());
            }
        }
        
        syncConfig.get().setLastSync(LocalDateTime.now());
        airbnbSyncRepository.save(syncConfig.get());
        
        logger.info("Sincronização concluída. " + processedCount + " processadas, " + errorCount + " erros.");
        
    } catch (Exception e) {
        logger.severe("Erro crítico na sincronização do Airbnb: " + e.getMessage());
        throw new RuntimeException("Falha na sincronização: " + e.getMessage(), e);
    }
}

private void debugReserveCreation(ReservesionRequest request) {
    try {
        logger.info("Debug createReserveExternal:");
        logger.info("Request: " + request.dates() + ", " + request.guestName() + ", " + request.roomNumber());
        
        Optional<Room> room = roomRepository.findByNumber(request.roomNumber());
        if (room.isPresent()) {
            logger.info("Quarto encontrado: " + room.get().getNumber());
        } else {
            logger.warning("Quarto não encontrado: " + request.roomNumber());
        }
        
    } catch (Exception e) {
        logger.warning("Erro no debug: " + e.getMessage());
    }
}

    private void processAirbnbReservation(AirbnbReservation airbnbReservation, AirbnbSync syncConfig) {
        try {
            Optional<AirbnbReservation> existing = airbnbReservationRepository
                .findByAirbnbReservationId(airbnbReservation.getAirbnbReservationId());
            
            if (existing.isPresent()) {
                logger.info("Reserva já processada: " + airbnbReservation.getAirbnbReservationId());
                return;
            }
            
            airbnbReservationRepository.save(airbnbReservation);
            
            try {
                createInternalReserve(airbnbReservation, syncConfig);
                airbnbReservation.setIsProcessed(true);
                logger.info("Reserva criada com sucesso: " + airbnbReservation.getAirbnbReservationId());
            } catch (Exception e) {
                airbnbReservation.setIsProcessed(false);
                logger.warning("Reserva salva mas não processada: " + airbnbReservation.getAirbnbReservationId() + " - " + e.getMessage());
            }
            
            airbnbReservationRepository.save(airbnbReservation);
            
        } catch (Exception e) {
            logger.warning("Erro ao processar reserva " + airbnbReservation.getAirbnbReservationId() + ": " + e.getMessage());
        }
    }
    

    private void createInternalReserve(AirbnbReservation airbnbReservation, AirbnbSync syncConfig) {
    try {
        Set<LocalDate> dates = getDatesBetween(airbnbReservation.getCheckIn(), airbnbReservation.getCheckOut());
        
        Integer roomNumber;
        Room targetRoom = null;
        
        if (syncConfig.getRoom() != null) {
            targetRoom = syncConfig.getRoom();
            roomNumber = targetRoom.getNumber();
            logger.info("Usando quarto associado: " + roomNumber + " (ID: " + targetRoom.getId() + ")");
        } else if (syncConfig.getRoomNumber() != null) {
            roomNumber = syncConfig.getRoomNumber();
            try {
                targetRoom = roomRepository.findByNumber(roomNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado: " + roomNumber));
                logger.info("Quarto encontrado pelo número: " + roomNumber);
            } catch (ResourceNotFoundException e) {
                logger.warning("Quarto " + roomNumber + " não encontrado. Criando reserva sem associação direta.");
                targetRoom = null;
            }
        } else {
            roomNumber = null;
            logger.warning("Nenhum quarto especificado. Usando quarto padrão: " + roomNumber);
        }
        
        String guestName = generateUniqueAirbnbGuestName(airbnbReservation);
        airbnbReservation.setGuestName(guestName);
        
        logger.info("🔍 Debug - Antes de criar reserva:");
        logger.info("📋 Datas: " + dates);
        logger.info("👤 Hóspede: " + guestName);
        logger.info("🏠 Quarto: " + roomNumber);

        ReservesionRequest request = new ReservesionRequest(
            dates,
            guestName,
            roomNumber
        );
        
        Reserve reserve;
        try {
            reserve = reserveService.createReserveForAirbnb(request);
            logger.info("Reserva Airbnb criada com sucesso: #" + reserve.getId());
        } catch (Exception e) {
            logger.severe("Erro crítico ao criar reserva Airbnb: " + e.getMessage());
            throw e;
        }
        
        airbnbReservation.setReserve(reserve);
        
        logger.info("Reserva Airbnb processada - " +
                   "ID: " + reserve.getId() + 
                   ", Hóspede: " + guestName + 
                   ", Quarto: " + roomNumber + 
                   ", Datas: " + dates.size() + " noites");
        
    } catch (Exception e) {
        logger.severe("Erro ao criar reserva interna: " + e.getMessage());
        throw new RuntimeException("Falha ao criar reserva interna: " + e.getMessage(), e);
    }
}



private String generateUniqueAirbnbGuestName(AirbnbReservation airbnbReservation) {
    String baseName = "Airbnb Hóspede";
    
    String reservationId = airbnbReservation.getAirbnbReservationId();
    
    String uniqueSuffix;
    if (reservationId.length() >= 16) {
        String firstPart = reservationId.substring(0, 8);
        String lastPart = reservationId.substring(reservationId.length() - 8);
        uniqueSuffix = firstPart + "-" + lastPart;
    } else {
        uniqueSuffix = reservationId;
    }
    
    return baseName + " - " + uniqueSuffix.toUpperCase();
}


private String generateAirbnbGuestEmail(String reservationId) {
    String cleanId = reservationId.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    String uniquePart = cleanId.length() > 10 ? cleanId.substring(0, 10) : cleanId;
    return "airbnb_" + uniquePart + "@elohostel.com";
}

@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
public void processSingleReservation(AirbnbReservation airbnbReservation, AirbnbSync syncConfig) {
    processAirbnbReservation(airbnbReservation, syncConfig);
}


private void extractAdditionalInfoFromDescription(String description, AirbnbReservation reservation) {
    try {
        if (description.contains("Reservation URL:")) {
            String url = description.substring(description.indexOf("Reservation URL:") + 16);
            if (url.contains("\n")) {
                url = url.substring(0, url.indexOf("\n"));
            }
            logger.info("URL da reserva: " + url.trim());
        }
        
        if (description.contains("Phone Number")) {
            String phoneInfo = description.substring(description.indexOf("Phone Number"));
            if (phoneInfo.contains("\n")) {
                phoneInfo = phoneInfo.substring(0, phoneInfo.indexOf("\n"));
            }
            logger.info("Informações de telefone: " + phoneInfo.trim());
        }
    } catch (Exception e) {
        logger.warning("Erro ao extrair informações da descrição: " + e.getMessage());
    }
}

    private boolean isRealReservation(String summary) {
    if (summary == null) return false;
    
    String lowerSummary = summary.toLowerCase();
    
    if (lowerSummary.contains("blocked") || 
        lowerSummary.contains("unavailable") ||
        lowerSummary.contains("bloqueado") ||
        lowerSummary.contains("indisponível") ||
        lowerSummary.contains("not available") ||
        lowerSummary.contains("(not available)")) {
        return false;
    }
    
    return lowerSummary.contains("reservation") || 
           lowerSummary.contains("reserva") ||
           lowerSummary.contains("reserved") ||
           containsGuestName(summary);
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
    if (summary == null) return "Hóspede Airbnb";
    
    if (summary.equals("Reserved")) {
        return "Hóspede Airbnb";
    }
    
    if (summary.toLowerCase().contains("reservation for")) {
        return summary.substring(summary.toLowerCase().indexOf("reservation for") + 15).trim();
    }
    
    String cleanSummary = summary.replace("Reserved", "")
                                .replace("Reservation", "")
                                .replace("Reserva", "")
                                .trim();
    
    return cleanSummary.isEmpty() ? "Hóspede Airbnb" : cleanSummary;
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
        logger.info("Executando sincronização automática do Airbnb...");
        
        List<AirbnbSync> activeSyncs = airbnbSyncRepository.findByIsActiveTrue();
        logger.info("Conexões ativas: " + activeSyncs.size());
        
        for (AirbnbSync sync : activeSyncs) {
            try {
                logger.info("🔍 Sincronizando: " + sync.getPropertyName());
                syncAirbnbReservations(sync.getIcalUrl(), sync.getPropertyId());
                logger.info("Concluído: " + sync.getPropertyId());
            } catch (Exception e) {
                logger.severe("Erro na sync para " + sync.getPropertyId() + ": " + e.getMessage());
            }
        }
        
        logger.info("🏁 Sincronização automática finalizada");
    }

    private AirbnbReservation parseEvent(String eventContent, AirbnbSync syncConfig) {
    try {
        AirbnbReservation reservation = new AirbnbReservation();
        reservation.setAirbnbSync(syncConfig);
        
        String uid = extractProperty(eventContent, "UID:");
        if (uid != null) {
            reservation.setAirbnbReservationId(uid);
        }
        
        String dtStart = extractProperty(eventContent, "DTSTART;VALUE=DATE:");
        String dtEnd = extractProperty(eventContent, "DTEND;VALUE=DATE:");
        
        if (dtStart != null && dtEnd != null) {
            reservation.setCheckIn(parseICalDate(dtStart));
            reservation.setCheckOut(parseICalDate(dtEnd));
        }
        
        String summary = extractProperty(eventContent, "SUMMARY:");
        if (summary != null && isRealReservation(summary)) {
            String uniqueGuestName = generateUniqueAirbnbGuestName(reservation);
            reservation.setGuestName(uniqueGuestName);
            
            logger.info("✅ Reserva detectada: " + uniqueGuestName + 
                       " - " + reservation.getCheckIn() + " a " + reservation.getCheckOut());
        } else {
            logger.info("🔒 Ignorando bloqueio: " + summary);
            return null;
        }
        
        String description = extractProperty(eventContent, "DESCRIPTION:");
        if (description != null) {
            extractAdditionalInfoFromDescription(description, reservation);
        }
        
        reservation.setStatus("CONFIRMED");
        reservation.setLastModified(LocalDateTime.now());
        reservation.setIsProcessed(false);
        
        return reservation;
        
    } catch (Exception e) {
        logger.warning("Erro ao parsear evento: " + e.getMessage());
        return null;
    }
    }
}