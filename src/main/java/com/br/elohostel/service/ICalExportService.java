package com.br.elohostel.service;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.br.elohostel.model.AirbnbSync;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Tenant;
import com.br.elohostel.model.enums.ReserveStatus;
import com.br.elohostel.repository.AirbnbSyncRepository;
import com.br.elohostel.repository.ReserveRepository;
import com.br.elohostel.repository.TenantRepository;
import com.br.elohostel.util.TenantContext;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;

@Service
public class ICalExportService {

    private final ReserveRepository reserveRepository;
    private final AirbnbSyncRepository airbnbSyncRepository;
    private final TenantRepository tenantRepository;

    public ICalExportService(ReserveRepository reserveRepository,
                           AirbnbSyncRepository airbnbSyncRepository,
                           TenantRepository tenantRepository) {
        this.reserveRepository = reserveRepository;
        this.airbnbSyncRepository = airbnbSyncRepository;
        this.tenantRepository = tenantRepository;
    }

    private String getCurrentTenantKey() {
        return TenantContext.getCurrentTenant();
    }

    private Tenant getCurrentTenant() {
        String tenantKey = getCurrentTenantKey();
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey);
        if (tenant == null) {
            throw new RuntimeException("Tenant não encontrado: " + tenantKey);
        }
        return tenant;
    }

    public String generateCalendarExportUrl(String propertyId) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return "/api/calendar/export/" + propertyId + "/" + token + ".ics";
    }

    public String generateExportUrl(String propertyId) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return "/api/ical/export/" + propertyId + "/" + token + ".ics";
    }

    public String generateICalContent(String propertyId) {
        String tenantKey = getCurrentTenantKey();
        
        try {
            Optional<AirbnbSync> syncConfig = airbnbSyncRepository.findByPropertyIdAndTenant_TenantKey(propertyId, tenantKey);
            if (syncConfig.isEmpty()) {
                throw new RuntimeException("Configuração não encontrada para: " + propertyId + " no tenant: " + tenantKey);
            }

            String calendarName = syncConfig.get().getCalendarName();
            Integer targetRoomNumber = syncConfig.get().getAssociatedRoomNumber();
            
            System.out.println("Exportando iCal para quarto: " + targetRoomNumber + " (tenant: " + tenantKey + ")");
            
            List<Reserve> allReserves = reserveRepository.findByTenant_TenantKey(tenantKey);
            
            List<Reserve> reservesToExport = allReserves.stream()
                .filter(reserve -> isReserveForTargetRoom(reserve, targetRoomNumber))
                .filter(reserve -> shouldExportReserve(reserve))
                .collect(Collectors.toList());

            System.out.println("Reservas para exportar: " + reservesToExport.size() + " de " + allReserves.size() + 
                              " (Quarto " + targetRoomNumber + ")");

            net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
            calendar.getProperties().add(new ProdId("-//Elohostel//iCal Export//" + tenantKey));
            calendar.getProperties().add(Version.VERSION_2_0);
            calendar.getProperties().add(CalScale.GREGORIAN);
            calendar.getProperties().add(new XProperty("X-WR-CALNAME", calendarName + " - " + tenantKey));
            calendar.getProperties().add(new XProperty("X-WR-CALDESC", "Reservas do Elohostel - " + tenantKey));

            int totalEvents = 0;
            for (Reserve reserve : reservesToExport) {
                try {
                    List<VEvent> events = createEventsFromReserve(reserve);
                    for (VEvent event : events) {
                        calendar.getComponents().add(event);
                        totalEvents++;
                    }
                    System.out.println(events.size() + " eventos adicionados para Reserva #" + reserve.getId());
                } catch (Exception e) {
                    System.err.println("Erro ao criar eventos para reserva " + reserve.getId() + ": " + e.getMessage());
                }
            }

            CalendarOutputter outputter = new CalendarOutputter();
            outputter.setValidating(false);
            StringWriter writer = new StringWriter();
            outputter.output(calendar, writer);
            
            String result = writer.toString();
            
            return result;

        } catch (Exception e) {
            System.err.println("Erro grave ao gerar iCal: " + e.getMessage());
            throw new RuntimeException("Erro ao gerar iCal: " + e.getMessage(), e);
        }
    }

    private boolean isReserveForTargetRoom(Reserve reserve, Integer targetRoomNumber) {
        try {
            if (targetRoomNumber == null) {
                return true; 
            }
            
            boolean hasTargetRoom = reserve.getRooms().stream()
                .anyMatch(room -> targetRoomNumber.equals(room.getNumber()));
                
            return hasTargetRoom;
            
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldExportReserve(Reserve reserve) {
        try {
            if (reserve.getReserveStatus() == ReserveStatus.CANCELLED) {
                return false;
            }
            
            if (reserve.getReservedDays() == null || reserve.getReservedDays().isEmpty()) {
                return false;
            }
            
            if (reserve.getRooms() == null || reserve.getRooms().isEmpty()) {
                return false;
            }
            
            // Verificar se a reserva pertence ao tenant atual
            if (!reserve.getTenant().getTenantKey().equals(getCurrentTenantKey())) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Erro ao verificar reserva " + reserve.getId() + ": " + e.getMessage());
            return false;
        }
    }

    private Integer getRoomNumberFromReserve(Reserve reserve) {
        try {
            return reserve.getRooms().iterator().next().getNumber();
        } catch (Exception e) {
            return null;
        }
    }

    private List<VEvent> createEventsFromReserve(Reserve reserve) {
        List<VEvent> events = new ArrayList<>();
        
        if (reserve.getReservedDays() == null || reserve.getReservedDays().isEmpty()) {
            System.out.println("Reserva #" + reserve.getId() + " não tem datas reservadas");
            return events;
        }

        try {
            for (LocalDate reservedDate : reserve.getReservedDays()) {
                VEvent event = createEventFromReserveForDate(reserve, reservedDate);
                if (event != null) {
                    events.add(event);
                }
            }
            
            System.out.println(events.size() + " eventos criados para Reserva #" + reserve.getId());
        } catch (Exception e) {
            System.err.println("Erro ao criar eventos para reserva " + reserve.getId() + ": " + e.getMessage());
        }

        return events;
    }

    private VEvent createEventFromReserveForDate(Reserve reserve, LocalDate reservedDate) {
        try {
            java.util.Calendar startCal = java.util.Calendar.getInstance();
            startCal.set(reservedDate.getYear(), reservedDate.getMonthValue() - 1, reservedDate.getDayOfMonth(), 
                        0, 0, 0);
            startCal.set(java.util.Calendar.MILLISECOND, 0);
            
            java.util.Calendar endCal = java.util.Calendar.getInstance();
            endCal.set(reservedDate.getYear(), reservedDate.getMonthValue() - 1, reservedDate.getDayOfMonth(), 
                      0, 0, 0);
            endCal.set(java.util.Calendar.MILLISECOND, 0);
            endCal.add(java.util.Calendar.DATE, 1);

            DateTime start = new DateTime(startCal.getTime());
            DateTime end = new DateTime(endCal.getTime());

            String guestName = reserve.getGuest().isEmpty() ? 
                "Hóspede Airbnb" : reserve.getGuest().iterator().next().getName();
            String roomNumber = reserve.getRooms().isEmpty() ? 
                "N/A" : String.valueOf(reserve.getRooms().iterator().next().getNumber());

            String summary = "Reservado - " + guestName;

            VEvent event = new VEvent(start, end, summary);

            StringBuilder description = new StringBuilder();
            description.append("Reserva #").append(reserve.getId())
                      .append(" - Hóspede: ").append(guestName)
                      .append(" - Quarto: ").append(roomNumber)
                      .append(" - Tenant: ").append(reserve.getTenant().getTenantKey());

            event.getProperties().add(new Description(description.toString()));
            event.getProperties().add(new Uid("elohostel-" + reserve.getTenant().getTenantKey() + "-" + reserve.getId() + "-" + reservedDate));
            event.getProperties().add(new DtStamp(new DateTime()));

            System.out.println("Evento criado para " + reservedDate + ": " + start + " até " + end);
            
            return event;

        } catch (Exception e) {
            System.err.println("Erro ao criar evento para reserva " + reserve.getId() + " na data " + reservedDate + ": " + e.getMessage());
            return null;
        }
    }

    public String forceICalUpdate(String propertyId) {
        String tenantKey = getCurrentTenantKey();
        
        try {
            System.out.println("Forçando atualização do iCal para: " + propertyId + " (tenant: " + tenantKey + ")");
            
            String newICalContent = generateICalContent(propertyId);
            
            System.out.println("iCal atualizado para: " + propertyId);
            System.out.println("Eventos: " + (newICalContent.split("BEGIN:VEVENT").length - 1));
            
            return newICalContent;
            
        } catch (Exception e) {
            System.err.println("Erro ao forçar atualização do iCal: " + e.getMessage());
            throw new RuntimeException("Falha ao atualizar iCal: " + e.getMessage(), e);
        }
    }

    public void debugICalContent(String propertyId) {
        String tenantKey = getCurrentTenantKey();
        try {
            String icalContent = generateICalContent(propertyId);
            
            System.out.println("=".repeat(50));
            System.out.println("DEBUG iCal para: " + propertyId + " (tenant: " + tenantKey + ")");
            System.out.println("=".repeat(50));
            System.out.println("Tamanho: " + icalContent.length() + " caracteres");
            System.out.println("Contém BEGIN:VEVENT: " + icalContent.contains("BEGIN:VEVENT"));
            
            String[] events = icalContent.split("BEGIN:VEVENT");
            int eventCount = events.length - 1;
            System.out.println("Número de eventos: " + eventCount);
            
            boolean hasValidDTSTART = icalContent.contains("DTSTART:");
            boolean hasValidDTEND = icalContent.contains("DTEND:");
            System.out.println("Tem DTSTART: " + hasValidDTSTART);
            System.out.println("Tem DTEND: " + hasValidDTEND);
            
            if (icalContent.contains("DTSTART:") && icalContent.contains("DTEND:")) {
                String[] lines = icalContent.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].contains("DTSTART:") || lines[i].contains("DTEND:")) {
                        System.out.println(lines[i]);
                    }
                }
            }
            
            System.out.println("=".repeat(50));
            
        } catch (Exception e) {
            System.err.println("Erro no debug: " + e.getMessage());
        }
    }

    public String generateICalContentForTenant(String propertyId, String tenantKey) {
        try {
            TenantContext.setCurrentTenant(tenantKey);
            
            return generateICalContent(propertyId);
            
        } finally {
            TenantContext.clear();
        }
    }

    public String forceICalUpdateForTenant(String propertyId, String tenantKey) {
        try {
            TenantContext.setCurrentTenant(tenantKey);
            return forceICalUpdate(propertyId);
        } finally {
            TenantContext.clear();
        }
    }
}