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
    public ResponseEntity<Reserve> insert(@RequestBody ReservesionRequest request) {
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
        System.out.println("Guest: " + nameGuest);
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

    // CORRIGIDO: endpoint para setar valor personalizado
    @PutMapping("/{id}/custom-value")
    public ResponseEntity<Reserve> setCustomValue(
            @PathVariable Long id, 
            @RequestBody BigDecimal customValue) {
        Reserve reserve = service.setCustomValue(id, customValue);
        return ResponseEntity.ok(reserve);
    }

    // CORRIGIDO: endpoint para voltar ao valor automático
    @PutMapping("/{id}/auto-value")
    public ResponseEntity<Reserve> setAutoValue(@PathVariable Long id) {
        Reserve reserve = service.setAutoValue(id);
        return ResponseEntity.ok(reserve);
    }

    // CORRIGIDO: endpoint para atualizar taxa de hóspede extra
    @PutMapping("/{id}/extra-guest-fee")
    public ResponseEntity<Reserve> updateExtraGuestFee(
            @PathVariable Long id, 
            @RequestBody BigDecimal newFee) {
        Reserve reserve = service.updateExtraGuestFee(id, newFee);
        return ResponseEntity.ok(reserve);
    }

    // CORRIGIDO: endpoint para obter detalhes do valor
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
        
        // Usando os nomes corretos dos métodos
        if (request.useCustomAmount() != null) {
            reserve.setUseCustomAmount(request.useCustomAmount());
        }
        
        if (request.customTotalAmount() != null) {
            reserve.setCustomTotalAmount(request.customTotalAmount());
        }
        
        if (request.extraGuestDailyFee() != null) {
            reserve.setExtraGuestDailyFee(request.extraGuestDailyFee());
        }
        
        // Recalcula o total se necessário
        reserve.calculateTotalAmount();
        
        Reserve updatedReserve = service.save(reserve);
        return ResponseEntity.ok(updatedReserve);
    }

    @PostMapping("/create-with-guest")
    public ResponseEntity<?> createReservationWithGuest(@RequestBody CreateReservationWithGuestRequest request) {
        try {
            if (request.guests() == null || request.guests().isEmpty()) {
                return ResponseEntity.badRequest().body("Pelo menos um hóspede deve ser informado");
            }
            
            if (request.roomNumber() == null) {
                return ResponseEntity.badRequest().body("Número do quarto é obrigatório");
            }
            
            if (request.dates() == null || request.dates().isEmpty()) {
                return ResponseEntity.badRequest().body("Pelo menos uma data deve ser informada");
            }
            
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
            return ResponseEntity.created(uri).body(reservation);
            
        } catch (ResourceNotFoundException e) {
            logger.severe("❌ Recurso não encontrado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recurso não encontrado: " + e.getMessage());
        } catch (IllegalStateException e) {
            logger.severe("❌ Conflito de disponibilidade: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.severe("❌ Dados inválidos: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.severe("❌ Erro interno ao criar reserva: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao criar reserva: " + e.getMessage());
        }
    }

    // CORRIGIDO: endpoint para atualizar dados da reserva
    @PutMapping("/up/{id}")
    public ResponseEntity<Reserve> up(@PathVariable Long id, @RequestBody UpdateDataReserveDTO dto) {
        var obj = service.reserveUpdateExtra(id, dto);
        return ResponseEntity.ok().body(obj);
    }

    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam Integer roomNumber,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut) {
        try {
            Map<String, Object> availability = service.checkAvailability(roomNumber, checkIn, checkOut);
            return ResponseEntity.ok(availability);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao verificar disponibilidade: " + e.getMessage());
        }
    }

    // CORRIGIDO: endpoint para atualizar valores da reserva com ajuste manual
    @PutMapping("/update-values/{id}")
    public ResponseEntity<Reserve> updateReserveValues(
            @PathVariable Long id,
            @RequestBody Map<String, Object> values) {
        
        Reserve reserve = service.findById(id);
        
        // Atualiza os campos com os novos nomes
        if (values.containsKey("dailyRate")) {
            BigDecimal dailyRate = new BigDecimal(values.get("dailyRate").toString());
            reserve.setDailyRate(dailyRate);
        } else if (values.containsKey("initialValue")) {
            // Mantém compatibilidade com o nome antigo
            BigDecimal dailyRate = new BigDecimal(values.get("initialValue").toString());
            reserve.setDailyRate(dailyRate);
        }
        
        if (values.containsKey("extraGuestDailyFee")) {
            BigDecimal extraFee = new BigDecimal(values.get("extraGuestDailyFee").toString());
            reserve.setExtraGuestDailyFee(extraFee);
        } else if (values.containsKey("extraGuestFee")) {
            // Mantém compatibilidade com o nome antigo
            BigDecimal extraFee = new BigDecimal(values.get("extraGuestFee").toString());
            reserve.setExtraGuestDailyFee(extraFee);
        }
        
        if (values.containsKey("manualAdjustment")) {
            BigDecimal adjustment = new BigDecimal(values.get("manualAdjustment").toString());
            BigDecimal calculatedValue = reserve.calculateTotalAmount().add(adjustment);
            reserve.setCustomTotalAmount(calculatedValue);
            reserve.setUseCustomAmount(true);
        }
        
        Reserve updatedReserve = service.save(reserve);
        return ResponseEntity.ok(updatedReserve);
    }

    // CORRIGIDO: endpoint para atualizar valores com ajuste manual
    @PutMapping("/update-values-with-adjustment/{id}")
    public ResponseEntity<Reserve> updateValuesWithAdjustment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> values) {
        
        Reserve reserve = service.findById(id);
        
        // Atualiza valores base com os novos nomes
        if (values.containsKey("dailyRate")) {
            BigDecimal dailyRate = new BigDecimal(values.get("dailyRate").toString());
            reserve.setDailyRate(dailyRate);
        } else if (values.containsKey("initialValue")) {
            // Mantém compatibilidade com o nome antigo
            BigDecimal dailyRate = new BigDecimal(values.get("initialValue").toString());
            reserve.setDailyRate(dailyRate);
        }
        
        if (values.containsKey("extraGuestDailyFee")) {
            BigDecimal extraFee = new BigDecimal(values.get("extraGuestDailyFee").toString());
            reserve.setExtraGuestDailyFee(extraFee);
        } else if (values.containsKey("extraGuestFee")) {
            // Mantém compatibilidade com o nome antigo
            BigDecimal extraFee = new BigDecimal(values.get("extraGuestFee").toString());
            reserve.setExtraGuestDailyFee(extraFee);
        }
        
        // Calcula o novo total com ajuste
        BigDecimal calculatedValue = reserve.calculateTotalAmount();
        
        if (values.containsKey("manualAdjustment")) {
            BigDecimal adjustment = new BigDecimal(values.get("manualAdjustment").toString());
            BigDecimal finalValue = calculatedValue.add(adjustment);
            reserve.setCustomTotalAmount(finalValue);
            reserve.setUseCustomAmount(true);
        } else {
            reserve.setUseCustomAmount(false);
            reserve.setCustomTotalAmount(null);
        }
        
        Reserve updatedReserve = service.save(reserve);
        return ResponseEntity.ok(updatedReserve);
    }
}