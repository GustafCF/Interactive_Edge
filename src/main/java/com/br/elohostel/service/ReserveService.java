package com.br.elohostel.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Bed;
import com.br.elohostel.model.BedOccupation;
import com.br.elohostel.model.Guest;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.RoomOccupation;
import com.br.elohostel.model.Tenant;
import com.br.elohostel.model.dtos.AddDatesRequest;
import com.br.elohostel.model.dtos.CreateReservationWithGuestRequest;
import com.br.elohostel.model.dtos.ReservesionRequest;
import com.br.elohostel.model.dtos.UpdateDataReserveDTO;
import com.br.elohostel.model.dtos.cancelReserveByGuestAndDatesRequest;
import com.br.elohostel.model.enums.BedStatus;
import com.br.elohostel.model.enums.ReserveStatus;
import com.br.elohostel.model.enums.RoomStatus;
import com.br.elohostel.repository.BedOccupationRepository;
import com.br.elohostel.repository.BedRepository;
import com.br.elohostel.repository.GuestRepository;
import com.br.elohostel.repository.ReserveRepository;
import com.br.elohostel.repository.RoomOccupationRepository;
import com.br.elohostel.repository.RoomRepository;
import com.br.elohostel.repository.TenantRepository;
import com.br.elohostel.util.TenantContext;

@Service
public class ReserveService {
    private static final Logger logger = Logger.getLogger(ReserveService.class.getName());

    private final ReserveRepository reserveRepo;
    private final GuestRepository guestRepo;
    private final RoomRepository roomRepo;
    private final BedRepository bedRepo;
    private final RoomOccupationRepository roomOccupationRepo;
    private final BedOccupationRepository bedOccupationRepo;
    private final TenantRepository tenantRepo;

    public ReserveService(ReserveRepository reserveRepo, GuestRepository guestRepo, 
                         RoomRepository roomRepo, BedRepository bedRepo, 
                         RoomOccupationRepository roomOccupationRepo, 
                         BedOccupationRepository bedOccupationRepo,
                         TenantRepository tenantRepo) {
        this.reserveRepo = reserveRepo;
        this.guestRepo = guestRepo;
        this.roomRepo = roomRepo;
        this.bedRepo = bedRepo;
        this.roomOccupationRepo = roomOccupationRepo;
        this.bedOccupationRepo = bedOccupationRepo;
        this.tenantRepo = tenantRepo;
    }

    private String getCurrentTenantKey() {
        return TenantContext.getCurrentTenant();
    }

    private Tenant getCurrentTenant() {
        String tenantKey = getCurrentTenantKey();
        Tenant tenant = tenantRepo.findByTenantKey(tenantKey);
        if (tenant == null) {
            throw new RuntimeException("Tenant não encontrado: " + tenantKey);
        }
        return tenant;
    }

    public List<Reserve> findAll() {
        return reserveRepo.findByTenant_TenantKey(getCurrentTenantKey());
    }

    public Reserve findById(Long id) {
        String tenantKey = getCurrentTenantKey();
        return reserveRepo.findByIdAndTenant_TenantKey(id, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Reserve save(Reserve reserve) {
        reserve.setTenant(getCurrentTenant());
        return reserveRepo.save(reserve);
    }

   @Transactional
    public Reserve createReservationWithGuest(CreateReservationWithGuestRequest request) {
        try {
            if (request.guests() == null || request.guests().isEmpty()) {
                throw new IllegalArgumentException("Pelo menos um hóspede deve ser informado");
            }
            String tenantKey = getCurrentTenantKey();

            List<Guest> guests = request.guests().stream()
                .map(this::findOrCreateGuestWithCompleteInfo)
                .collect(Collectors.toList());
            
            Room room = roomRepo.findByNumberAndTenant_TenantKey(request.roomNumber(), tenantKey)
                    .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado: " + request.roomNumber()));
            
            int numberOfGuests = guests.size();
            validateDatesAvailability(room, request.dates(), numberOfGuests);

            Reserve reserve = new Reserve();
            reserve.setReservedDays(request.dates());
            reserve.setReserveStatus(ReserveStatus.CONFIRMED);
            reserve.setTenant(getCurrentTenant());
            
            guests.forEach(reserve.getGuest()::add);
            reserve.getRooms().add(room);
            
            reserve.setDailyRate(room.getPrice());
            reserve.setUseCustomAmount(false);
            
            reserve.calculateTotalAmount();

            Reserve savedReserve = reserveRepo.save(reserve);

            createOccupations(savedReserve, room, request.dates(), numberOfGuests);

            guests.forEach(guest -> {
                guest.getReservation().add(savedReserve);
                guestRepo.save(guest);
            });

            return savedReserve;

        } catch (Exception e) {
            logger.severe("Erro na criação de reserva: " + e.getMessage());
            throw new RuntimeException("Falha na criação de reserva: " + e.getMessage(), e);
        }
    }

    private Guest findOrCreateGuestWithCompleteInfo(CreateReservationWithGuestRequest.GuestInfo guestInfo) {
        String tenantKey = getCurrentTenantKey();
        Tenant tenant = getCurrentTenant();
        try {
            Optional<Guest> existingGuest = guestRepo.findByNameAndTenant_TenantKey(guestInfo.name(), tenantKey);
            
            if (existingGuest.isPresent()) {
                Guest guest = existingGuest.get();
                boolean updated = false;
                
                if (guestInfo.rg() != null && !guestInfo.rg().trim().isEmpty() && 
                    (guest.getRg() == null || !guest.getRg().equals(guestInfo.rg()))) {
                    guest.setRg(guestInfo.rg());
                    updated = true;
                }
                
                if (guestInfo.phone() != null && !guestInfo.phone().trim().isEmpty() && 
                    (guest.getPhone() == null || !guest.getPhone().equals(guestInfo.phone()))) {
                    guest.setPhone(guestInfo.phone());
                    updated = true;
                }
                
                if (guestInfo.email() != null && !guestInfo.email().trim().isEmpty() && 
                    (guest.getEmail() == null || !guest.getEmail().equals(guestInfo.email()))) {
                    guest.setEmail(guestInfo.email());
                    updated = true;
                }
                
                if (updated) {
                    guest = guestRepo.save(guest);
                }
                
                return guest;
            }
            
            Guest newGuest = new Guest();
            newGuest.setName(guestInfo.name());
            newGuest.setRg(guestInfo.rg() != null ? guestInfo.rg() : "Não informado");
            newGuest.setPhone(guestInfo.phone() != null ? guestInfo.phone() : "Não informado");
            newGuest.setEmail(guestInfo.email() != null ? guestInfo.email() : "");
            newGuest.setTenant(tenant);
            
            Guest savedGuest = guestRepo.save(newGuest);
            return savedGuest;
            
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar hóspede: " + e.getMessage(), e);
        }
    }

private void validateDatesAvailability(Room room, Set<LocalDate> dates, int numberOfGuests) {
    String tenantKey = getCurrentTenantKey();
    if (room.isExclusiveRoom() || room.isStudio() || room.isSuite()) {
        boolean conflict = roomOccupationRepo.findAll().stream()
            .filter(ro -> ro.getRoom().equals(room))
            .anyMatch(ro -> ro.getOccupiedDays().stream().anyMatch(dates::contains));
            
        if (conflict) {
            throw new IllegalStateException("Quarto " + room.getNumber() + " já está reservado para algumas das datas selecionadas.");
        }
    } else if (room.isSharedRoom() || room.isSharedBathroom()) {
        long availableBeds = bedRepo.findByRoomAndTenant_TenantKey(room, tenantKey).stream()
            .filter(bed -> bedOccupationRepo.findConflicts(bed, dates, tenantKey).isEmpty())
            .count();

        if (availableBeds < numberOfGuests) {
            throw new IllegalStateException(
                String.format("Quarto compartilhado %s tem apenas %d cama(s) disponível(is) para as datas selecionadas, mas são necessárias %d cama(s) para %d hóspede(s).",
                    room.getNumber(), availableBeds, numberOfGuests, numberOfGuests)
            );
        }
    }
}

private void createOccupations(Reserve reserve, Room room, Set<LocalDate> dates, int numberOfGuests) {
    String tenantKey = getCurrentTenantKey();
    if (room.isExclusiveRoom() || room.isStudio() || room.isSuite()) {
        RoomOccupation ro = new RoomOccupation();
        ro.setRoom(room);
        ro.setReserve(reserve);
        ro.getOccupiedDays().addAll(dates);
        ro.setTenant(room.getTenant());
        roomOccupationRepo.save(ro);
        
    } else if (room.isSharedRoom() || room.isSharedBathroom()) {
        // 🔥 CORREÇÃO: Buscar MÚLTIPLAS camas disponíveis
        List<Bed> availableBeds = findAvailableBedsForDatesInRoom(room, dates, numberOfGuests);
        
        if (availableBeds.size() < numberOfGuests) {
            throw new IllegalStateException(
                String.format("Não há camas suficientes no quarto %s. Necessárias: %d, Disponíveis: %d",
                    room.getNumber(), numberOfGuests, availableBeds.size())
            );
        }

        // Criar uma ocupação para CADA cama
        for (Bed bed : availableBeds) {
            BedOccupation bo = new BedOccupation();
            bo.setBed(bed);
            bo.setReserve(reserve);
            bo.getOccupiedDays().addAll(dates);
            bo.setTenant(bed.getTenant());
            bedOccupationRepo.save(bo);
        }
    }
}

private List<Bed> findAvailableBedsForDatesInRoom(Room room, Set<LocalDate> dates, int bedsNeeded) {
    String tenantKey = getCurrentTenantKey();

    List<Bed> availableBeds = bedRepo.findByRoomAndTenant_TenantKey(room, tenantKey)
        .stream()
        .filter(bed -> !hasBedOccupationConflict(bed, dates, tenantKey))
        .collect(Collectors.toList());
    
    if (availableBeds.size() < bedsNeeded) {
        return new ArrayList<>();  // Retorna lista vazia se não tiver camas suficientes
    }
    
    // Retorna as primeiras N camas disponíveis
    return availableBeds.subList(0, bedsNeeded);
}


    private Bed findAvailableBedForDatesInRoom(Room room, Set<LocalDate> dates) {
        String tenantKey = getCurrentTenantKey();

        List<Bed> availableBeds = bedRepo.findByRoomAndTenant_TenantKey(room, tenantKey)
            .stream()
            .filter(bed -> bed.getBedStatus() == BedStatus.VAGUE)
            .filter(bed -> !hasBedOccupationConflict(bed, dates, tenantKey))
            .collect(Collectors.toList());
        
        if (availableBeds.isEmpty()) {
            availableBeds = bedRepo.findByRoomAndTenant_TenantKey(room, tenantKey)
                .stream()
                .filter(bed -> !hasBedOccupationConflict(bed, dates, tenantKey))
                .collect(Collectors.toList());
        }
        
        return availableBeds.isEmpty() ? null : availableBeds.get(0);
    }

    private boolean hasBedOccupationConflict(Bed bed, Set<LocalDate> dates, String tenantKey) {
        List<BedOccupation> conflicts = bedOccupationRepo.findConflicts(bed, dates, tenantKey);
        
        for (BedOccupation occupation : conflicts) {
            for (LocalDate date : dates) {
                if (occupation.getOccupiedDays().contains(date)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional
    public Reserve createReserve(ReservesionRequest request) {
        String tenantKey = getCurrentTenantKey();
        var guest = guestRepo.findByNameAndTenant_TenantKey(request.guestName(), tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException(request.guestName()));
        var room = roomRepo.findByNumberAndTenant_TenantKey(request.roomNumber(), tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException(request.roomNumber()));
        int numberOfGuests = 1;
        validateDatesAvailability(room, request.dates(), numberOfGuests);

        Reserve reserve = new Reserve();
        reserve.setReservedDays(request.dates());
        reserve.setReserveStatus(ReserveStatus.CONFIRMED);
        reserve.setTenant(getCurrentTenant());
        reserve.getGuest().add(guest);
        reserve.getRooms().add(room);
        reserve.setDailyRate(room.getPrice());
        reserve.setUseCustomAmount(false);
        reserve.calculateTotalAmount();
        Reserve savedReserve = reserveRepo.save(reserve);

        if (room.isExclusiveRoom() || room.isStudio() || room.isSuite()) {
            boolean conflit = roomOccupationRepo.findAll().stream()
                .filter(ro -> ro.getRoom().equals(room))
                .anyMatch(ro -> ro.getOccupiedDays().stream().anyMatch(request.dates()::contains));
            if (conflit) throw new IllegalStateException("Room " + room.getNumber() + " is already reserved for these dates.");

            RoomOccupation ro = new RoomOccupation();
            ro.setRoom(room);
            ro.setReserve(savedReserve);
            ro.getOccupiedDays().addAll(request.dates());
            roomOccupationRepo.save(ro);
    }

    if (room.isSharedRoom() || room.isSharedBathroom()) {
        List<Bed> availableBeds = findAvailableBedsForDatesInRoom(room, request.dates(), numberOfGuests);
        
        if (availableBeds.size() < numberOfGuests) {
            throw new IllegalStateException("No available beds in shared room " + room.getNumber() + " for the selected dates");
        }
        
        // Criar ocupação para cada cama (no caso, 1 cama)
        for (Bed bed : availableBeds) {
            BedOccupation bo = new BedOccupation();
            bo.setBed(bed);
            bo.setReserve(savedReserve);
            bo.getOccupiedDays().addAll(request.dates());
            bo.setTenant(bed.getTenant());
            bedOccupationRepo.save(bo);
        }
    }

    guest.getReservation().add(savedReserve);
    guestRepo.save(guest);

    return savedReserve;
}

   @Transactional
public Reserve createReserveForAirbnb(ReservesionRequest request) {
    String tenantKey = getCurrentTenantKey();
    Tenant tenant = getCurrentTenant();
    try {
        Guest guest = findOrCreateAirbnbGuest(request.guestName());
        
        var room = roomRepo.findByNumberAndTenant_TenantKey(request.roomNumber(), tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado: " + request.roomNumber()));

        // Airbnb também é 1 hóspede = 1 cama
        int numberOfGuests = 1;
        validateDatesAvailability(room, request.dates(), numberOfGuests);

        Reserve reserve = new Reserve();
        reserve.setReservedDays(request.dates());
        reserve.setReserveStatus(ReserveStatus.CONFIRMED);
        reserve.setTenant(getCurrentTenant());
        reserve.getGuest().add(guest);
        reserve.getRooms().add(room);
        
        reserve.setDailyRate(room.getPrice());
        reserve.setUseCustomAmount(false);
        
        reserve.calculateTotalAmount();

        Reserve savedReserve = reserveRepo.save(reserve);

        if (room.isExclusiveRoom() || room.isStudio() || room.isSuite()) {
            boolean conflit = roomOccupationRepo.findAll().stream()
                .filter(ro -> ro.getRoom().equals(room))
                .anyMatch(ro -> ro.getOccupiedDays().stream().anyMatch(request.dates()::contains));
                
            if (conflit) {
                throw new IllegalStateException("Quarto " + room.getNumber() + " já está reservado para estas datas.");
            }

            RoomOccupation ro = new RoomOccupation();
            ro.setRoom(room);
            ro.setReserve(savedReserve);
            ro.getOccupiedDays().addAll(request.dates());
            ro.setTenant(room.getTenant());
            roomOccupationRepo.save(ro);
        }

        if (room.isSharedRoom() || room.isSharedBathroom()) {
            List<Bed> availableBeds = findAvailableBedsForDatesInRoom(room, request.dates(), numberOfGuests);
            
            if (availableBeds.size() < numberOfGuests) {
                throw new IllegalStateException("Nenhuma cama disponível no quarto compartilhado " + room.getNumber());
            }

            for (Bed bed : availableBeds) {
                BedOccupation bo = new BedOccupation();
                bo.setBed(bed);
                bo.setReserve(savedReserve);
                bo.getOccupiedDays().addAll(request.dates());
                bo.setTenant(tenant);
                bedOccupationRepo.save(bo);
            }
        }
        guest.getReservation().add(savedReserve);
        guestRepo.save(guest);
        return savedReserve;

    } catch (Exception e) {
        throw new RuntimeException("Falha na criação de reserva Airbnb: " + e.getMessage(), e);
    }
}

    private Guest findOrCreateAirbnbGuest(String guestName) {
        String tenantKey = getCurrentTenantKey();
        Tenant tenant = getCurrentTenant();
        try {
            Optional<Guest> existingGuest = guestRepo.findByNameAndTenant_TenantKey(guestName, tenantKey);
            
            if (existingGuest.isPresent()) {
                return existingGuest.get();
            }

            Guest newGuest = new Guest();
            newGuest.setName(guestName);
            newGuest.setEmail(generateAirbnbGuestEmail(guestName));
            newGuest.setPhone("Não informado");
            newGuest.setRg("Airbnb-" + System.currentTimeMillis());
            newGuest.setTenant(tenant);
            Guest savedGuest = guestRepo.save(newGuest);
            
            return savedGuest;
            
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar hóspede Airbnb: " + e.getMessage(), e);
        }
    }

    private String generateAirbnbGuestEmail(String guestName) {
        String cleanName = guestName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return "airbnb_" + cleanName + "_" + timestamp + "@elohostel.com";
    }

    public Reserve addGuestForReserve(Long id, String nameGuest) {
        String tenantKey = getCurrentTenantKey();
        Reserve reserve = findById(id);
        Guest guest = guestRepo.findByNameAndTenant_TenantKey(nameGuest, tenantKey).orElseThrow(() -> new ResourceNotFoundException(nameGuest));
        reserve.getGuest().add(guest);
        reserveRepo.save(reserve);
        guest.getReservation().add(reserve);
        guestRepo.save(guest);
        return reserve;        
    }

    public Reserve removeGuestForReserve(String nameGuest, Long id) {
        String tenantKey = getCurrentTenantKey();
        Reserve reserve = findById(id);
        Guest guest = guestRepo.findByNameAndTenant_TenantKey(nameGuest, tenantKey).orElseThrow(() -> new ResourceNotFoundException(nameGuest));
        reserve.getGuest().remove(guest);
        reserveRepo.save(reserve);
        guest.getReservation().remove(reserve);
        guestRepo.save(guest);
        return reserve;
    }

    // CORRIGIDO: usando setCustomTotalAmount e setUseCustomAmount
    public Reserve setCustomValue(Long reserveId, BigDecimal customValue) {
        Reserve reserve = findById(reserveId);
        reserve.setCustomTotalAmount(customValue);
        reserve.setUseCustomAmount(true);
        reserve.calculateTotalAmount();
        return reserveRepo.save(reserve);
    }

    // CORRIGIDO: usando setCustomTotalAmount e calculateTotalAmount
    public Reserve setAutoValue(Long reserveId) {
        Reserve reserve = findById(reserveId);
        reserve.setUseCustomAmount(false);
        reserve.setCustomTotalAmount(null);
        reserve.calculateTotalAmount();
        return reserveRepo.save(reserve);
    }

    // CORRIGIDO: usando setExtraGuestDailyFee e calculateTotalAmount
    public Reserve updateExtraGuestFee(Long reserveId, BigDecimal newFee) {
        Reserve reserve = findById(reserveId);
        reserve.setExtraGuestDailyFee(newFee);
        reserve.calculateTotalAmount();
        return reserveRepo.save(reserve);
    }

    // CORRIGIDO: usando os getters e métodos novos
    public Map<String, Object> getValueDetails(Long reserveId) {
        Reserve reserve = findById(reserveId);
        
        Map<String, Object> details = new HashMap<>();
        details.put("reserveId", reserve.getId());
        details.put("roomBaseValue", reserve.getDailyRate());
        details.put("numberOfDays", reserve.getNumberOfDays());
        details.put("numberOfGuests", reserve.getGuest().size());
        details.put("numberOfExtraGuests", reserve.getNumberOfExtraGuests());
        details.put("extraGuestFee", reserve.getExtraGuestDailyFee());
        details.put("useCustomValue", reserve.getUseCustomAmount());
        details.put("customValue", reserve.getCustomTotalAmount());
        details.put("calculatedTotal", reserve.calculateTotalAmount());
        
        if (Boolean.FALSE.equals(reserve.getUseCustomAmount())) {
            BigDecimal baseValue = reserve.getDailyRate() != null ? reserve.getDailyRate() : BigDecimal.ZERO;
            int numberOfDays = reserve.getNumberOfDays();
            int extraGuests = reserve.getNumberOfExtraGuests();
            
            BigDecimal dailyTotal = baseValue.multiply(BigDecimal.valueOf(numberOfDays));
            BigDecimal extraFees = reserve.getExtraGuestDailyFee().multiply(BigDecimal.valueOf(extraGuests * numberOfDays));
            
            details.put("calculationBreakdown", Map.of(
                "dailyTotal", dailyTotal,
                "extraFees", extraFees,
                "formula", "(" + baseValue + " × " + numberOfDays + ") + (" + 
                        reserve.getExtraGuestDailyFee() + " × " + extraGuests + " × " + numberOfDays + ")"
            ));
        }
        
        return details;
    }

    @Transactional
    public Reserve updateReserveDates(Long reserveId, Set<LocalDate> newDates) {
        Reserve reserve = findById(reserveId);

        validateReserveCanBeModified(reserve);

        for (LocalDate newDate : newDates) {
            validateNewDate(newDate);
            validateAvailability(reserve, newDate);
        }

        reserve.setReservedDays(newDates);
        reserve.calculateTotalAmount();
        updateOccupations(reserve, newDates);

        return reserveRepo.save(reserve);
    }

    @Transactional
    public Reserve cancelReserve(Long reserveId) {
        Reserve reserve = findById(reserveId);

        if (reserve.getReserveStatus() == ReserveStatus.CANCELLED) {
            throw new IllegalStateException("Reserve is already cancelled");
        }

        if (!reserve.getCheckIn().isEmpty()) {
            throw new IllegalStateException("Cannot cancel reserve after check-in");
        }

        reserve.setReserveStatus(ReserveStatus.CANCELLED);
        Reserve cancelledReserve = reserveRepo.save(reserve);

        removeOccupations(reserve);

        return cancelledReserve;
    }

    private void removeOccupations(Reserve reserve) {
        String tenantKey = getCurrentTenantKey();
        List<RoomOccupation> roomOccupations = roomOccupationRepo.findByReserve(reserve);
        roomOccupationRepo.deleteAll(roomOccupations);

        List<BedOccupation> bedOccupations = bedOccupationRepo.findByReserveAndTenant_TenantKey(reserve, tenantKey);
        bedOccupationRepo.deleteAll(bedOccupations);
    }

    @Transactional
    public Reserve cancelReserveByGuestAndDates(cancelReserveByGuestAndDatesRequest request) {
        String tenantKey = getCurrentTenantKey();
        var guest = guestRepo.findByNameAndTenant_TenantKey(request.guestName(), tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + request.guestName()));

        Reserve reserve = reserveRepo.findAll().stream()
                .filter(r -> r.getGuest().contains(guest))
                .filter(r -> r.getReservedDays().equals(request.dates()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Reserve not found for guest: " + request.guestName() + " on dates: " + request.dates()));

        if (!reserve.getTenant().getTenantKey().equals(getCurrentTenantKey())) {
            throw new ResourceNotFoundException("Reserva não encontrada no tenant atual");
        }

        return cancelReserve(reserve.getId());
    }

    // CORRIGIDO: usando setDailyRate e calculateTotalAmount
    public Reserve addRoom(Long reserveId, Integer roomNumber) {
        String tenantKey = getCurrentTenantKey();
        Reserve reserve = findById(reserveId);
        Room room = roomRepo.findByNumberAndTenant_TenantKey(roomNumber, tenantKey).orElseThrow(() -> new ResourceNotFoundException(roomNumber));    

        if (reserve.getReserveStatus() == ReserveStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a cancelled reserve");
        }

        reserve.getRooms().add(room);
        
        if (reserve.getRooms().size() == 1) {
            reserve.setDailyRate(room.getPrice());
            reserve.calculateTotalAmount();
        }
        
        reserveRepo.save(reserve);
        room.getReservation().add(reserve);
    
        return reserve;
    }

    // CORRIGIDO: usando setDailyRate e calculateTotalAmount
    public Reserve removeRoom(Long reserveId, Integer roomNumber) {
        String tenantKey = getCurrentTenantKey();
        Reserve reserve = findById(reserveId);
        Room room = roomRepo.findByNumberAndTenant_TenantKey(roomNumber, tenantKey).orElseThrow(() -> new ResourceNotFoundException(roomNumber));    

        if (reserve.getReserveStatus() == ReserveStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a cancelled reserve");
        }

        reserve.getRooms().remove(room);
        
        if (reserve.getRooms().isEmpty()) {
            reserve.setDailyRate(BigDecimal.ZERO);
            reserve.calculateTotalAmount();
        }
        
        reserveRepo.save(reserve);
        room.getReservation().remove(reserve);
        roomRepo.save(room);
        return reserve;
    }

    @Transactional
    public Reserve addDate(Long reserveId, LocalDate newDate) {
        Reserve reserve = findById(reserveId);
        validateReserveCanBeModified(reserve);
        validateNewDate(newDate);
        validateAvailability(reserve, newDate);
        Set<LocalDate> updatedDates = new HashSet<>(reserve.getReservedDays());
        if (updatedDates.contains(newDate)) {
            throw new IllegalStateException("Date " + newDate + " is already included in the reservation");
        }

        updatedDates.add(newDate);
        reserve.setReservedDays(updatedDates);

        reserve.calculateTotalAmount();
        updateOccupationsForNewDate(reserve, newDate);

        Reserve updatedReserve = reserveRepo.save(reserve);
        return updatedReserve;
    }

    private void validateReserveCanBeModified(Reserve reserve) {
        if (reserve.getReserveStatus() == ReserveStatus.CANCELLED) {
            throw new IllegalStateException("Cannot modify a cancelled reserve");
        }
    }

    private void validateNewDate(LocalDate newDate) {
        LocalDate today = LocalDate.now();
        
        if (newDate.isBefore(today)) {
            throw new IllegalArgumentException("Cannot add date in the past: " + newDate);
        }
        LocalDate maxDate = today.plusYears(1);
        if (newDate.isAfter(maxDate)) {
            throw new IllegalArgumentException("Cannot reserve dates more than 1 year in advance: " + newDate);
        }
    }

    private void validateAvailability(Reserve reserve, LocalDate newDate) {
        Room room = reserve.getRooms().iterator().next();
        
        if (room.isExclusiveRoom() || room.isStudio() || room.isSuite()) {
            validateExclusiveRoomAvailability(room, newDate, reserve.getId());
        } else if (room.isSharedRoom() || room.isSharedBathroom()) {
            validateSharedRoomAvailability(room, newDate, reserve.getId());
        } else {
            throw new IllegalStateException("Unknown room type for room: " + room.getNumber());
        }
    }

    private void validateExclusiveRoomAvailability(Room room, LocalDate date, Long currentReserveId) {
        boolean conflict = roomOccupationRepo.findAll().stream()
                .filter(ro -> ro.getRoom().equals(room))
                .filter(ro -> !ro.getReserve().getId().equals(currentReserveId))
                .anyMatch(ro -> ro.getOccupiedDays().contains(date));
        
        if (conflict) {
            throw new IllegalStateException("Room " + room.getNumber() + " is already occupied on " + date);
        }
    }

    private void validateSharedRoomAvailability(Room room, LocalDate date, Long currentReserveId) {
        long availableBeds = bedRepo.findAll().stream()
                .filter(bed -> bed.getRoom().equals(room))
                .filter(bed -> isBedAvailableOnDate(bed, date, currentReserveId))
                .count();

        if (availableBeds == 0) {
            throw new IllegalStateException("No available beds in shared room " + room.getNumber() + " on " + date);
        }
    }

    private boolean isBedAvailableOnDate(Bed bed, LocalDate date, Long currentReserveId) {
        return bedOccupationRepo.findAll().stream()
                .filter(bo -> bo.getBed().equals(bed))
                .filter(bo -> !bo.getReserve().getId().equals(currentReserveId))
                .noneMatch(bo -> bo.getOccupiedDays().contains(date));
    }

    private void updateOccupationsForNewDate(Reserve reserve, LocalDate newDate) {
        Room room = reserve.getRooms().iterator().next();

        if (room.isExclusiveRoom() || room.isStudio() || room.isSuite()) {
            updateRoomOccupationForNewDate(reserve, newDate);
        } else if (room.isSharedRoom() || room.isSharedBathroom()) {
            updateBedOccupationForNewDate(reserve, newDate);
        }
    }

    private void updateRoomOccupationForNewDate(Reserve reserve, LocalDate newDate) {
        RoomOccupation roomOccupation = roomOccupationRepo.findByReserve(reserve)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    RoomOccupation newRo = new RoomOccupation();
                    newRo.setRoom(reserve.getRooms().iterator().next());
                    newRo.setReserve(reserve);
                    newRo.setTenant(newRo.getRoom().getTenant());
                    return newRo;
                });
        roomOccupation.getOccupiedDays().add(newDate);
        roomOccupationRepo.save(roomOccupation);
    }

    private void updateBedOccupationForNewDate(Reserve reserve, LocalDate newDate) {
        String tenantKey = getCurrentTenantKey();
        Room room = reserve.getRooms().iterator().next();    
        BedOccupation bedOccupation = bedOccupationRepo.findByReserveAndTenant_TenantKey(reserve, tenantKey)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Bed availableBed = findAvailableBedForDate(room, newDate, reserve.getId());
                    BedOccupation newBo = new BedOccupation();
                    newBo.setBed(availableBed);
                    newBo.setReserve(reserve);
                    return newBo;
                });

        if (!isBedAvailableOnDate(bedOccupation.getBed(), newDate, reserve.getId())) {
            Bed newAvailableBed = findAvailableBedForDate(room, newDate, reserve.getId());
            bedOccupation.setBed(newAvailableBed);
            bedOccupation.setTenant(newAvailableBed.getTenant());
        }

        bedOccupation.getOccupiedDays().add(newDate);
        bedOccupationRepo.save(bedOccupation);
    }

    private Bed findAvailableBedForDate(Room room, LocalDate date, Long currentReserveId) {
        return bedRepo.findAll().stream()
                .filter(bed -> bed.getRoom().equals(room))
                .filter(bed -> isBedAvailableOnDate(bed, date, currentReserveId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No available beds found for date: " + date));
    }

    public Reserve removeDate(Long reserveId, LocalDate date) {
        Reserve reserve = findById(reserveId);
        reserve.getReservedDays().remove(date);
        reserve.calculateTotalAmount();
        reserveRepo.save(reserve);    
        return reserve;
    }
    
    @Transactional
    public Reserve addDates(Long reserveId, AddDatesRequest newDates) {
        Reserve reserve = findById(reserveId);

        validateReserveCanBeModified(reserve);

        for (LocalDate newDate : newDates.dates()) {
            validateNewDate(newDate);
            validateAvailability(reserve, newDate);
        }

        Set<LocalDate> updatedDates = new HashSet<>(reserve.getReservedDays());
        for (LocalDate newDate : newDates.dates()) {
            if (updatedDates.contains(newDate)) {
                throw new IllegalStateException("Date " + newDate + " is already included in the reservation");
            }
            updatedDates.add(newDate);
        }
        reserve.setReservedDays(updatedDates);
        reserve.calculateTotalAmount();
        for (LocalDate newDate : newDates.dates()) {
            updateOccupationsForNewDate(reserve, newDate);
        }
        Reserve updatedReserve = reserveRepo.save(reserve);
        return updatedReserve;
    }

    private void updateOccupations(Reserve reserve, Set<LocalDate> newDates) {
        String tenantKey = getCurrentTenantKey();
        removeOccupations(reserve);
        Room room = reserve.getRooms().iterator().next();
        if (room.isExclusiveRoom() || room.isStudio() || room.isSuite()) {
            boolean conflict = roomOccupationRepo.findAll().stream()
                    .filter(ro -> ro.getRoom().equals(room))
                    .anyMatch(ro -> ro.getOccupiedDays().stream().anyMatch(newDates::contains));
            
            if (conflict) {
                throw new IllegalStateException("Room " + room.getNumber() + " is already reserved for the new dates");
            }
            RoomOccupation ro = new RoomOccupation();
            ro.setRoom(room);
            ro.setReserve(reserve);
            ro.getOccupiedDays().addAll(newDates);
            ro.setTenant(room.getTenant());
            roomOccupationRepo.save(ro);
        }

        if (room.isSharedRoom() || room.isSharedBathroom()) {
            Bed availableBed = bedRepo.findAll().stream()
                    .filter(b -> b.getRoom().equals(room))
                    .filter(b -> bedOccupationRepo.findConflicts(b, newDates, tenantKey).isEmpty())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No available beds in shared room " + room.getNumber() + " for the new dates"));
            BedOccupation bo = new BedOccupation();
            bo.setBed(availableBed);
            bo.setReserve(reserve);
            bo.getOccupiedDays().addAll(newDates);
            bedOccupationRepo.save(bo);
        }
    }

    @Transactional
    public Reserve checkIn(Long id) {
        String tenantKey = getCurrentTenantKey();        
        Reserve reserva = findById(id);
        reserva.getCheckIn().add(LocalDateTime.now());

        var rooms = reserva.getRooms();
        for (Room room : rooms) {
            if (room.isExclusiveRoom() || room.isStudio() || room.isSuite()) {
                room.setRoomStatus(RoomStatus.OCCUPIED);
            } else if (room.isSharedRoom() || room.isSharedBathroom()) {
                List<BedOccupation> bedOccupations = bedOccupationRepo.findByReserveAndTenant_TenantKey(reserva, tenantKey);
                for (BedOccupation bo : bedOccupations) {
                    Bed bed = bo.getBed();
                    bed.setBedStatus(BedStatus.OCCUPIED);
                    bedRepo.save(bed);
                }

                boolean allBedsOccupied = room.getBeds().stream()
                        .allMatch(b -> b.getBedStatus() == BedStatus.OCCUPIED);
                
                if (allBedsOccupied) {
                    room.setRoomStatus(RoomStatus.OCCUPIED);
                }
            }
        }
        roomRepo.saveAll(rooms);

        return reserveRepo.save(reserva);
    }

    @Transactional
    public Reserve checkout(Long id) {
        Reserve reserve = findById(id);
        reserve.getCheckOut().add(LocalDateTime.now());

        var rooms = reserve.getRooms();
        for (Room room : rooms) {
            if (room.isExclusiveRoom() || room.isStudio() || room.isSuite()) {
                room.setRoomStatus(RoomStatus.VAGUE);
            } else if (room.isSharedRoom() || room.isSharedBathroom()) {
                Bed reservedBed = findBedForReserveInRoom(reserve, room);
                if (reservedBed != null) {
                    reservedBed.setBedStatus(BedStatus.VAGUE);
                    bedRepo.save(reservedBed);
                    updateSharedRoomStatus(room);
                }
            }
        }
        roomRepo.saveAll(rooms);
        
        return reserveRepo.save(reserve);
    }

    private Bed findBedForReserveInRoom(Reserve reserve, Room room) {
        String tenantKey = getCurrentTenantKey();
        try {
            List<BedOccupation> bedOccupations = bedOccupationRepo.findByReserveAndTenant_TenantKey(reserve, tenantKey);
            
            return bedOccupations.stream()
                    .filter(bo -> bo.getBed().getRoom().equals(room))
                    .map(BedOccupation::getBed)
                    .findFirst()
                    .orElse(null);
                    
        } catch (Exception e) {
            logger.warning("Erro ao buscar cama para reserva " + reserve.getId() + " no quarto " + room.getNumber());
            return null;
        }
    }

    private void updateSharedRoomStatus(Room room) {
        if (!room.isSharedRoom()) return;
        
        long occupiedBeds = room.getBeds().stream()
                .filter(bed -> bed.getBedStatus() == BedStatus.OCCUPIED)
                .count();
        
        long totalBeds = room.getBeds().size();
        
        if (occupiedBeds == totalBeds) {
            if (room.getRoomStatus() != RoomStatus.OCCUPIED) {
                room.setRoomStatus(RoomStatus.OCCUPIED);
            }
        } else if (occupiedBeds == 0) {
            if (room.getRoomStatus() != RoomStatus.VAGUE) {
                room.setRoomStatus(RoomStatus.VAGUE);
            }
        } 
        roomRepo.save(room);
    }

    public Map<String, Object> checkAvailability(Integer roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String tenantKey = getCurrentTenantKey();
        validateDates(checkIn, checkOut);
        
        Set<LocalDate> requestedDates = getDatesBetween(checkIn, checkOut);
        
        Room room = roomRepo.findByNumberAndTenant_TenantKey(roomNumber, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado: " + roomNumber));
        boolean isAvailable;
        String message;
        String roomTypeDescription = room.getRoomTypeDescription();
        
        if (room.isAnyExclusiveType() || room.isStudio() || room.isSuite()) {
            boolean hasConflict = roomOccupationRepo.existsConflictForRoomAndDates(room, requestedDates);
            
            isAvailable = !hasConflict;
            message = hasConflict ? 
                String.format("%s não disponível para as datas solicitadas", roomTypeDescription) : 
                String.format("%s disponível", roomTypeDescription);
                
        } else if (room.isSharedRoom() || room.isSharedBathroom()) {
            long availableBeds = bedRepo.findByRoomAndBedStatusAndTenant_TenantKey(room, BedStatus.VAGUE, tenantKey).stream()
                    .filter(bed -> bedOccupationRepo.isBedAvailableForDates(bed, requestedDates, tenantKey))
                    .count();
            
            isAvailable = availableBeds > 0;
            message = isAvailable ? 
                String.format("%d cama(s) disponível(is) no %s", availableBeds, roomTypeDescription.toLowerCase()) :
                String.format("Nenhuma cama disponível no %s", roomTypeDescription.toLowerCase());
                
        } else {
            isAvailable = false;
            message = "Tipo de quarto não reconhecido";
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("available", isAvailable);
        response.put("message", message);
        response.put("roomNumber", roomNumber);
        response.put("roomType", roomTypeDescription);
        response.put("roomTypeEnum", room.getRoomType().name());
        response.put("roomStatus", room.getRoomStatus() != null ? room.getRoomStatus().name() : "UNKNOWN");
        response.put("price", room.getPrice());
        response.put("requestedDates", new ArrayList<>(requestedDates)); 
        response.put("checkIn", checkIn);
        response.put("checkOut", checkOut);
        response.put("numberOfNights", requestedDates.size());
        return response;
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in e check-out são obrigatórios");
        }
        
        LocalDate today = LocalDate.now();
        if (checkIn.isBefore(today)) {
            throw new IllegalArgumentException("Check-in não pode ser no passado");
        }
        
        if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
            throw new IllegalArgumentException("Data de check-in deve ser anterior à data de check-out");
        }
        
        LocalDate maxDate = today.plusYears(1);
        if (checkOut.isAfter(maxDate)) {
            throw new IllegalArgumentException("Reservas não podem ser feitas para mais de 1 ano no futuro");
        }
    }

    private Set<LocalDate> getDatesBetween(LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> dates = new HashSet<>();
        LocalDate currentDate = startDate;
        
        while (currentDate.isBefore(endDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        
        return dates;
    }

    @Transactional
    public void delete(Long id) {
        String tenantKey = getCurrentTenantKey();
        try {
            Reserve reserve = findById(id);            
            List<RoomOccupation> roomOccupations = roomOccupationRepo.findByReserve(reserve);
            if (!roomOccupations.isEmpty()) {
                roomOccupationRepo.deleteAll(roomOccupations);
            }

            List<BedOccupation> bedOccupations = bedOccupationRepo.findByReserveAndTenant_TenantKey(reserve, tenantKey);
            if (!bedOccupations.isEmpty()) {
                bedOccupationRepo.deleteAll(bedOccupations);
            }

            if (reserve.getReservedDays() != null && !reserve.getReservedDays().isEmpty()) {
                reserve.getReservedDays().clear();
                reserveRepo.save(reserve);
            }

            if (reserve.getGuest() != null && !reserve.getGuest().isEmpty()) {
                for (Guest guest : new ArrayList<>(reserve.getGuest())) {
                    guest.getReservation().remove(reserve);
                    reserve.getGuest().remove(guest);
                }
                guestRepo.saveAll(reserve.getGuest());
            }
            if (reserve.getRooms() != null && !reserve.getRooms().isEmpty()) {
                for (Room room : new ArrayList<>(reserve.getRooms())) {
                    room.getReservation().remove(reserve);
                    reserve.getRooms().remove(room);
                }
                roomRepo.saveAll(reserve.getRooms());
            }
            reserveRepo.saveAndFlush(reserve);
            reserveRepo.delete(reserve);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao excluir reserva: " + e.getMessage(), e);
        }
    }
    
    // CORRIGIDO: usando os setters novos
    public Reserve reserveUpdateExtra(Long id, UpdateDataReserveDTO entity) {
        Reserve reserve = findById(id);
        updateData(reserve, entity);
        return reserveRepo.save(reserve);
    }

    // CORRIGIDO: usando os setters novos
    private void updateData(Reserve entity, UpdateDataReserveDTO obj) {
        if (obj.customValue() != null) {
            entity.setCustomTotalAmount(obj.customValue());
        }
        if (obj.extraGuestFee() != null) {
            entity.setExtraGuestDailyFee(obj.extraGuestFee());
        }
        if (obj.useCustomValue() != null) {
            entity.setUseCustomAmount(obj.useCustomValue());
        }
        if (obj.initialValue() != null) {
            entity.setDailyRate(obj.initialValue());
        }
    }
}