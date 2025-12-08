package com.br.elohostel.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.dtos.AddDatesRequest;
import com.br.elohostel.model.dtos.CreateReservationWithGuestRequest;
import com.br.elohostel.model.dtos.ReservesionRequest;
import com.br.elohostel.model.dtos.UpdateDataReserveDTO;
import com.br.elohostel.model.dtos.UpdateReserveValueRequest;
import com.br.elohostel.service.ReserveService;

@RestController
@RequestMapping("/reserve")
public class ReserveController {

    private static final Logger logger = Logger.getLogger(ReserveController.class.getName());


    private final ReserveService service;

    public ReserveController(ReserveService service) {
        this.service = service;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Reserve>> getAllReservations() {
        List<Reserve> reservations = service.findAll();
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Reserve> findById(@PathVariable Long id) {
        var obj = service.findById(id);
        return ResponseEntity.ok().body(obj);
    }

    @PostMapping("/insert")
    public ResponseEntity<Reserve> insert(@RequestBody ReservesionRequest request){
        var obj = service.createReserve(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getId()).toUri();
        return ResponseEntity.created(uri).body(obj);
    }

    @PutMapping("/cancele/{id}")
    public ResponseEntity<Reserve> canceledReserve(@PathVariable Long id) {
        var obj = service.cancelReserve(id);
        return ResponseEntity.ok().body(obj);
    }

    @PutMapping("/{id}/add-date")
    public ResponseEntity<Reserve> addDate(@PathVariable Long id, @RequestBody LocalDate newDate) {
        var obj = service.addDate(id, newDate);
        return ResponseEntity.ok().body(obj);
    }

    @PutMapping("/{id}/add-dates")
    public ResponseEntity<Reserve> addDates(@PathVariable Long id, @RequestBody AddDatesRequest newDates) {
        var obj = service.addDates(id, newDates);
        return ResponseEntity.ok().body(obj);
    }

    @PutMapping("/remove-date/{id}")
    public ResponseEntity<Reserve> removeDate(@PathVariable Long id, @RequestParam("date") LocalDate date) {
        var obj = service.removeDate(id, date);
        return ResponseEntity.ok().body(obj);
    }

    @PutMapping("/add-room/{id}")
    public ResponseEntity<Reserve> addRoom(@PathVariable Long id, @RequestParam("roomNumber") Integer roomNumber) {
        var obj = service.addRoom(id, roomNumber);
        return ResponseEntity.ok().body(obj);
    }

    @PutMapping("/remove-room/{id}")
    public ResponseEntity<Reserve> removeRoom(@PathVariable Long id, @RequestParam("roomNumber") Integer roomNumber) {
        var obj = service.removeRoom(id, roomNumber);
        return ResponseEntity.ok().body(obj);
    }

    @PutMapping("/add-guest/{id}")
    public ResponseEntity<Reserve> addGuest(@PathVariable Long id, @RequestParam("nameGuest") String nameGuest) {
        System.out.println("Guest 3:" + nameGuest);
        var reserve = service.addGuestForReserve(id, nameGuest);
        return ResponseEntity.ok().body(reserve);
    }

    @PutMapping("/remove-guest/{id}")
    public ResponseEntity<Reserve> removeGuest(@PathVariable Long id, @RequestParam("nameGuest") String guestName) {
        var reserve = service.removeGuestForReserve(guestName, id);
        return ResponseEntity.ok().body(reserve);
    }

    @PutMapping("/check-in/{id}")
    public ResponseEntity<Reserve> checkIn(@PathVariable Long id) {
        var reserve = service.checkIn(id);
        return ResponseEntity.ok().body(reserve);
    }

    @PutMapping("/check-out/{id}")
    public ResponseEntity<Reserve> checkOut(@PathVariable Long id) {
        var reserve = service.checkout(id);
        return ResponseEntity.ok().body(reserve);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/custom-value")
    public ResponseEntity<Reserve> setCustomValue(
            @PathVariable Long id, 
            @RequestBody BigDecimal customValue) {
        Reserve reserve = service.setCustomValue(id, customValue);
        return ResponseEntity.ok(reserve);
    }

    @PutMapping("/{id}/auto-value")
    public ResponseEntity<Reserve> setAutoValue(@PathVariable Long id) {
        Reserve reserve = service.setAutoValue(id);
        return ResponseEntity.ok(reserve);
    }

    @PutMapping("/{id}/extra-guest-fee")
    public ResponseEntity<Reserve> updateExtraGuestFee(
            @PathVariable Long id, 
            @RequestBody BigDecimal newFee) {
        Reserve reserve = service.updateExtraGuestFee(id, newFee);
        return ResponseEntity.ok(reserve);
    }

    @GetMapping("/{id}/value-details")
    public ResponseEntity<Map<String, Object>> getValueDetails(@PathVariable Long id) {
        Map<String, Object> details = service.getValueDetails(id);
        return ResponseEntity.ok(details);
    }

    @PutMapping("/{id}/value")
    public ResponseEntity<Reserve> updateReserveValue(
            @PathVariable Long id,
            @RequestBody UpdateReserveValueRequest request) {
        
        Reserve reserve = service.findById(id);
        
        if (request.getUseCustomValue() != null) {
            reserve.setUseCustomValue(request.getUseCustomValue());
        }
        
        if (request.getCustomValue() != null) {
            reserve.setCustomValue(request.getCustomValue());
        }
        
        if (request.getExtraGuestFee() != null) {
            reserve.setExtraGuestFee(request.getExtraGuestFee());
        }
        
        Reserve updatedReserve = service.save(reserve);
        return ResponseEntity.ok(updatedReserve);
    }


@PostMapping("/create-with-guest")
public ResponseEntity<?> createReservationWithGuest(@RequestBody CreateReservationWithGuestRequest request) {
    try {
        logger.info("📥 Recebendo requisição para criar reserva com " + 
                   (request.guests() != null ? request.guests().size() : 0) + " hóspedes");
        
        // ✅ VALIDAÇÕES ATUALIZADAS
        if (request.guests() == null || request.guests().isEmpty()) {
            return ResponseEntity.badRequest().body("Pelo menos um hóspede deve ser informado");
        }
        
        if (request.roomNumber() == null) {
            return ResponseEntity.badRequest().body("Número do quarto é obrigatório");
        }
        
        if (request.dates() == null || request.dates().isEmpty()) {
            return ResponseEntity.badRequest().body("Pelo menos uma data deve ser informada");
        }

        // ✅ VALIDAR DADOS DOS HÓSPEDES
        for (int i = 0; i < request.guests().size(); i++) {
            var guest = request.guests().get(i);
            if (guest.name() == null || guest.name().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Nome do hóspede " + (i + 1) + " é obrigatório");
            }
            if (guest.rg() == null || guest.rg().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("RG do hóspede " + (i + 1) + " é obrigatório");
            }
        }

        Reserve reservation = service.createReservationWithGuest(request);
        
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(reservation.getId())
                .toUri();
                
        // logger.info("✅ Reserva criada com sucesso: #{} com {} hóspedes", 
        //            reservation.getId(), request.guests().size());
        
        return ResponseEntity.created(uri).body(reservation);
        
    } catch (ResourceNotFoundException e) {
        // logger.error("❌ Recurso não encontrado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recurso não encontrado: " + e.getMessage());
    } catch (IllegalStateException e) {
        // logger.error("❌ Conflito de disponibilidade: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    } catch (IllegalArgumentException e) {
        // logger.error("❌ Dados inválidos: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
        // logger.error("❌ Erro interno ao criar reserva: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro interno ao criar reserva: " + e.getMessage());
    }
}

    @PutMapping("/up/{id}")
    public ResponseEntity<Reserve> up(@PathVariable Long id, @RequestBody UpdateDataReserveDTO dto) {
        var obj = service.reserveUpdateExtra(id, dto);
        return ResponseEntity.ok().body(obj);
    }

}