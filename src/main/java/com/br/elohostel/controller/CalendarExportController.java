package com.br.elohostel.controller;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.Guest;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.enums.ReserveStatus;
import com.br.elohostel.repository.ReserveRepository;
import com.br.elohostel.service.ICalExportService;

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

@RestController
@RequestMapping("/api/calendar")
public class CalendarExportController {

    private final ICalExportService icalExportService;
    private final ReserveRepository reserveRepository;

    public CalendarExportController(ICalExportService icalExportService,
                                  ReserveRepository reserveRepository) {
        this.icalExportService = icalExportService;
        this.reserveRepository = reserveRepository;
    }

    /**
     * Endpoint específico para exportar o calendário do front-end que você mostrou
     */
    @GetMapping("/export/{propertyId}/{token}.ics")
    public ResponseEntity<String> exportCalendarICal(
            @PathVariable String propertyId,
            @PathVariable String token) {
        
        try {
            System.out.println("📅 Exportando calendário para property: " + propertyId);
            
            // Validar token
            if (!isValidToken(propertyId, token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token inválido");
            }

            // Buscar TODAS as reservas (não apenas confirmadas)
            List<Reserve> allReserves = reserveRepository.findAll();
            
            // Filtrar reservas que não estão canceladas
            List<Reserve> activeReserves = allReserves.stream()
                .filter(reserve -> reserve.getReserveStatus() != ReserveStatus.CANCELLED)
                .collect(Collectors.toList());

            // Criar calendário
            net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
            calendar.getProperties().add(new ProdId("-//Elohostel Calendar//iCal Export//PT"));
            calendar.getProperties().add(Version.VERSION_2_0);
            calendar.getProperties().add(CalScale.GREGORIAN);
            calendar.getProperties().add(new XProperty("X-WR-CALNAME", "Calendário Elohostel - Reservas"));

            // Adicionar cada reserva como evento
            for (Reserve reserve : activeReserves) {
                List<VEvent> events = createEventsFromReserve(reserve);
                events.forEach(event -> calendar.getComponents().add(event));
            }

            // Gerar conteúdo iCal
            CalendarOutputter outputter = new CalendarOutputter();
            StringWriter writer = new StringWriter();
            outputter.output(calendar, writer);
            
            System.out.println("✅ Calendário exportado com " + activeReserves.size() + " reservas ativas");

            return ResponseEntity.ok()
                    .header("Content-Type", "text/calendar; charset=utf-8")
                    .header("Content-Disposition", "inline; filename=\"elohostel-calendar.ics\"")
                    .body(writer.toString());

        } catch (Exception e) {
            System.err.println("❌ Erro ao exportar calendário: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao gerar calendário: " + e.getMessage());
        }
    }

    /**
     * Cria eventos a partir de uma reserva (pode ter múltiplas datas)
     */
    private List<VEvent> createEventsFromReserve(Reserve reserve) {
        List<VEvent> events = new ArrayList<>();
        
        if (reserve.getReservedDays() == null || reserve.getReservedDays().isEmpty()) {
            return events;
        }

        try {
            // Para cada data reservada, criar um evento
            for (LocalDate reservedDate : reserve.getReservedDays()) {
                VEvent event = createEventForDate(reserve, reservedDate);
                if (event != null) {
                    events.add(event);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao criar eventos para reserva " + reserve.getId() + ": " + e.getMessage());
        }

        return events;
    }

    /**
     * Cria um evento para uma data específica
     */
    /**
 * Cria um evento para uma data específica - VERSÃO CORRIGIDA
 */
private VEvent createEventForDate(Reserve reserve, LocalDate reservedDate) {
    try {
        // Criar data de início (meia-noite do dia da reserva)
        Calendar startCal = Calendar.getInstance();
        startCal.set(reservedDate.getYear(), reservedDate.getMonthValue() - 1, reservedDate.getDayOfMonth(), 
                    0, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        
        // ✅ CORREÇÃO CRÍTICA: Data de fim deve ser meia-noite do PRÓXIMO dia
        Calendar endCal = Calendar.getInstance();
        endCal.set(reservedDate.getYear(), reservedDate.getMonthValue() - 1, reservedDate.getDayOfMonth(), 
                  0, 0, 0);
        endCal.set(Calendar.MILLISECOND, 0);
        endCal.add(Calendar.DATE, 1); // ✅ PRÓXIMO DIA - isso cria duração de 24h

        DateTime start = new DateTime(startCal.getTime());
        DateTime end = new DateTime(endCal.getTime());

        // Criar título do evento
        String guestName = "Hóspede não informado";
        if (reserve.getGuest() != null && !reserve.getGuest().isEmpty()) {
            Guest guest = reserve.getGuest().iterator().next();
            guestName = guest.getName() != null ? guest.getName() : guestName;
        }

        String roomInfo = "Quarto não definido";
        if (reserve.getRooms() != null && !reserve.getRooms().isEmpty()) {
            Room room = reserve.getRooms().iterator().next();
            roomInfo = "Quarto " + (room.getNumber() != null ? room.getNumber() : "N/A");
        }

        // ✅ CORREÇÃO: Summary mais claro para o Airbnb
        String summary = "RESERVADO - " + guestName + " - Quarto " + roomInfo;

        VEvent event = new VEvent(start, end, summary);

        // Adicionar descrição detalhada
        StringBuilder description = new StringBuilder();
        description.append("Reserva #").append(reserve.getId()).append("\\n");
        description.append("Hóspede: ").append(guestName).append("\\n");
        description.append("Quarto: ").append(roomInfo).append("\\n");
        description.append("Status: ").append(reserve.getReserveStatus()).append("\\n");
        description.append("Data: ").append(reservedDate.toString()).append("\\n");
        
        if (reserve.getCheckIn() != null && !reserve.getCheckIn().isEmpty()) {
            description.append("Check-in: ").append(reserve.getCheckIn().get(0)).append("\\n");
        }
        if (reserve.getCheckOut() != null && !reserve.getCheckOut().isEmpty()) {
            description.append("Check-out: ").append(reserve.getCheckOut().get(0)).append("\\n");
        }

        event.getProperties().add(new Description(description.toString()));
        event.getProperties().add(new Uid("elohostel-" + reserve.getId() + "-" + reservedDate));
        
        // ✅ CORREÇÃO: Adicionar DTSTAMP apenas uma vez
        event.getProperties().add(new DtStamp(new DateTime()));

        System.out.println("✅ Evento criado: " + reservedDate + " (" + start + " to " + end + ")");
        
        return event;

    } catch (Exception e) {
        System.err.println("❌ Erro ao criar evento para reserva " + reserve.getId() + " na data " + reservedDate + ": " + e.getMessage());
        return null;
    }
}

    private boolean isValidToken(String propertyId, String token) {
        // Para testes, aceita qualquer token
        // Em produção, implemente validação segura
        return true;
    }
}