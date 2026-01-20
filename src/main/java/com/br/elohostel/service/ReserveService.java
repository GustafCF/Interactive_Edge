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

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Bed;
import com.br.elohostel.model.BedOccupation;
import com.br.elohostel.model.Guest;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.RoomOccupation;
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

import jakarta.transaction.Transactional;

@Service
public class ReserveService {
    private static final Logger logger = Logger.getLogger(ReserveService.class.getName());

    private final ReserveRepository reserveRepo;
    private final GuestRepository guestRepo;
    private final RoomRepository roomRepo;
    private final BedRepository bedRepo;
    private final RoomOccupationRepository roomOccupationRepo;
    private final BedOccupationRepository bedOccupationRepo;

    public ReserveService(ReserveRepository reserveRepo, GuestRepository guestRepo, 
                         RoomRepository roomRepo, BedRepository bedRepo, RoomOccupationRepository roomOccupationRepo, BedOccupationRepository bedOccupationRepo) {
        this.reserveRepo = reserveRepo;
        this.guestRepo = guestRepo;
        this.roomRepo = roomRepo;
        this.bedRepo = bedRepo;
        this.roomOccupationRepo = roomOccupationRepo;
        this.bedOccupationRepo = bedOccupationRepo;
    }

    public List<Reserve> findAll(){
        return reserveRepo.findAll();
    }

    public Reserve findById(Long id) {
        var obj = reserveRepo.findById(id);
        return obj.orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Reserve save(Reserve reserve) {
        return reserveRepo.save(reserve);
    }

    @Transactional
    public Reserve createReservationWithGuest(CreateReservationWithGuestRequest request) {
        try {
            if (request.guests() == null || request.guests().isEmpty()) {
                throw new IllegalArgumentException("Pelo menos um hóspede deve ser informado");
            }

            List<Guest> guests = request.guests().stream()
                .map(this::findOrCreateGuestWithCompleteInfo)
                .collect(Collectors.toList());
            
            Guest mainGuest = guests.get(0);
            Room room = roomRepo.findByNumber(request.roomNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado: " + request.roomNumber()));

            validateDatesAvailability(room, request.dates());

            Reserve reserve = new Reserve();
            reserve.setReservedDays(request.dates());
            reserve.setReserveStatus(ReserveStatus.CONFIRMED);
            
            guests.forEach(reserve.getGuest()::add);
            reserve.getRooms().add(room);
            
            reserve.setInitialValue(room.getPrice());
            reserve.setUseCustomValue(false);
            
            BigDecimal totalValue = reserve.calculateTotalValue();
            logger.info("💰 Valor calculado para reserva: " + totalValue + " (Hóspedes extras: " + (guests.size() - 1) + ")");

            Reserve savedReserve = reserveRepo.save(reserve);
            logger.info("📝 Reserva base criada: #" + savedReserve.getId() + " com " + guests.size() + " hóspedes");

            createOccupations(savedReserve, room, request.dates());

            guests.forEach(guest -> {
                guest.getReservation().add(savedReserve);
                guestRepo.save(guest);
            });

            logger.info("🎉 Reserva criada com sucesso: #" + savedReserve.getId() + 
                    " com " + guests.size() + " hóspedes");
            
            return savedReserve;

        } catch (Exception e) {
            logger.severe("❌ Erro ao criar reserva com hóspedes: " + e.getMessage());
            throw new RuntimeException("Falha na criação de reserva: " + e.getMessage(), e);
        }
    }

    private Guest findOrCreateGuestWithCompleteInfo(CreateReservationWithGuestRequest.GuestInfo guestInfo) {
        try {
            Optional<Guest> existingGuest = guestRepo.findByName(guestInfo.name());
            
            if (existingGuest.isPresent()) {
                Guest guest = existingGuest.get();
                logger.info("👤 Hóspede já existe, atualizando dados: " + guestInfo.name());
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
                    logger.info("Dados do hóspede atualizados: " + guestInfo.name());
                }
                
                return guest;
            }
            
            Guest newGuest = new Guest();
            newGuest.setName(guestInfo.name());
            newGuest.setRg(guestInfo.rg() != null ? guestInfo.rg() : "Não informado");
            newGuest.setPhone(guestInfo.phone() != null ? guestInfo.phone() : "Não informado");
            newGuest.setEmail(guestInfo.email() != null ? guestInfo.email() : "");
            
            Guest savedGuest = guestRepo.save(newGuest);
            return savedGuest;
            
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar hóspede: " + e.getMessage(), e);
        }
    }

    private void validateDatesAvailability(Room room, Set<LocalDate> dates) {
        if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
            boolean conflict = roomOccupationRepo.findAll().stream()
                .filter(ro -> ro.getRoom().equals(room))
                .anyMatch(ro -> ro.getOccupiedDays().stream().anyMatch(dates::contains));
                
            if (conflict) {
                throw new IllegalStateException("Quarto " + room.getNumber() + " já está reservado para algumas das datas selecionadas.");
            }
        } else if (room.isSharedRoom()) {
            long availableBeds = bedRepo.findAll().stream()
                .filter(b -> b.getRoom().equals(room))
                .filter(b -> bedOccupationRepo.findConflicts(b, dates).isEmpty())
                .count();

            if (availableBeds == 0) {
                throw new IllegalStateException("Nenhuma cama disponível no quarto compartilhado " + room.getNumber() + " para as datas selecionadas.");
            }
        }
    }

    private void createOccupations(Reserve reserve, Room room, Set<LocalDate> dates) {
        if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
            RoomOccupation ro = new RoomOccupation();
            ro.setRoom(room);
            ro.setReserve(reserve);
            ro.getOccupiedDays().addAll(dates);
            roomOccupationRepo.save(ro);
            
        } else if (room.isSharedRoom()) {
            Bed availableBed = bedRepo.findAll().stream()
                .filter(b -> b.getRoom().equals(room))
                .filter(b -> bedOccupationRepo.findConflicts(b, dates).isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma cama disponível no quarto compartilhado " + room.getNumber()));

            BedOccupation bo = new BedOccupation();
            bo.setBed(availableBed);
            bo.setReserve(reserve);
            bo.getOccupiedDays().addAll(dates);
            bedOccupationRepo.save(bo);
        }
    }

    @Transactional
    public Reserve createReserve(ReservesionRequest request) {
        var guest = guestRepo.findByName(request.guestName())
                .orElseThrow(() -> new ResourceNotFoundException(request.guestName()));

        var room = roomRepo.findByNumber(request.roomNumber())
                .orElseThrow(() -> new ResourceNotFoundException(request.roomNumber()));

        Reserve reserve = new Reserve();
        reserve.setReservedDays(request.dates());
        reserve.setReserveStatus(ReserveStatus.CONFIRMED);
        reserve.getGuest().add(guest);
        reserve.getRooms().add(room);
        
        reserve.setInitialValue(room.getPrice());
        reserve.setUseCustomValue(false);
        
        BigDecimal totalValue = reserve.calculateTotalValue();
        logger.info("💰 Valor calculado para reserva: " + totalValue + " (Base: " + room.getPrice() + 
                   ", Dias: " + reserve.getNumberOfDays() + ", Hóspedes extras: " + reserve.getNumberOfExtraGuests() + ")");

        Reserve savedReserve = reserveRepo.save(reserve);

        if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
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

        if (room.isSharedRoom()) {
            Bed availableBed = bedRepo.findAll().stream()
                .filter(b -> b.getRoom().equals(room))
                .filter(b -> b.getBedStatus() == BedStatus.VAGUE)
                .filter(b -> bedOccupationRepo.findConflicts(b, request.dates()).isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No available beds in shared room " + room.getNumber()));

            BedOccupation bo = new BedOccupation();
            bo.setBed(availableBed);
            bo.setReserve(savedReserve);
            bo.getOccupiedDays().addAll(request.dates());
            bedOccupationRepo.save(bo);
        }

        guest.getReservation().add(savedReserve);
        guestRepo.save(guest);

        return savedReserve;
    }

    @Transactional
    public Reserve createReserveForAirbnb(ReservesionRequest request) {
        try {
            Guest guest = findOrCreateAirbnbGuest(request.guestName());
            
            var room = roomRepo.findByNumber(request.roomNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado: " + request.roomNumber()));

            Reserve reserve = new Reserve();
            reserve.setReservedDays(request.dates());
            reserve.setReserveStatus(ReserveStatus.CONFIRMED);
            reserve.getGuest().add(guest);
            reserve.getRooms().add(room);
            
            reserve.setInitialValue(room.getPrice());
            reserve.setUseCustomValue(false);
            
            BigDecimal totalValue = reserve.calculateTotalValue();

            Reserve savedReserve = reserveRepo.save(reserve);

            if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
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
                roomOccupationRepo.save(ro);
            }

            if (room.isSharedRoom()) {
                Bed availableBed = bedRepo.findAll().stream()
                    .filter(b -> b.getRoom().equals(room))
                    .filter(b -> bedOccupationRepo.findConflicts(b, request.dates()).isEmpty())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Nenhuma cama disponível no quarto compartilhado " + room.getNumber()));

                BedOccupation bo = new BedOccupation();
                bo.setBed(availableBed);
                bo.setReserve(savedReserve);
                bo.getOccupiedDays().addAll(request.dates());
                bedOccupationRepo.save(bo);
            }

            guest.getReservation().add(savedReserve);
            guestRepo.save(guest);

            logger.info("🎉 Reserva Airbnb criada com sucesso: #" + savedReserve.getId());
            return savedReserve;

        } catch (Exception e) {
            throw new RuntimeException("Falha na criação de reserva Airbnb: " + e.getMessage(), e);
        }
    }

    private Guest findOrCreateAirbnbGuest(String guestName) {
        try {
            Optional<Guest> existingGuest = guestRepo.findByName(guestName);
            
            if (existingGuest.isPresent()) {
                logger.info("Hóspede Airbnb já existe: " + guestName);
                return existingGuest.get();
            }

            Guest newGuest = new Guest();
            newGuest.setName(guestName);
            newGuest.setEmail(generateAirbnbGuestEmail(guestName));
            newGuest.setPhone("Não informado");
            newGuest.setRg("Airbnb-" + System.currentTimeMillis());
            
            Guest savedGuest = guestRepo.save(newGuest);
            logger.info("Novo hóspede Airbnb criado: " + guestName);
            
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
        System.out.println("Guest 1:" + nameGuest);
        Guest guest = guestRepo.findByName(nameGuest).orElseThrow(() -> new ResourceNotFoundException(nameGuest));
        Reserve reserve = reserveRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        reserve.getGuest().add(guest);
        
        BigDecimal newTotal = reserve.calculateTotalValue();
        
        System.out.println("Guest 2: " + guest);
        reserveRepo.save(reserve);
        guest.getReservation().add(reserve);
        guestRepo.save(guest);
        return reserve;        
    }

    public Reserve removeGuestForReserve(String nameGuest, Long id) {
        Guest guest = guestRepo.findByName(nameGuest).orElseThrow(() -> new ResourceNotFoundException(nameGuest));
        Reserve reserve = reserveRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        reserve.getGuest().remove(guest);
        BigDecimal newTotal = reserve.calculateTotalValue();
        
        reserveRepo.save(reserve);
        guest.getReservation().remove(reserve);
        guestRepo.save(guest);
        return reserve;
    }

    public Reserve setCustomValue(Long reserveId, BigDecimal customValue) {
        Reserve reserve = reserveRepo.findById(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException(reserveId));
        
        reserve.setCustomValue(customValue);
        reserve.setUseCustomValue(true);
        
        return reserveRepo.save(reserve);
    }

    public Reserve setAutoValue(Long reserveId) {
        Reserve reserve = reserveRepo.findById(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException(reserveId));
        
        reserve.setUseCustomValue(false);
        reserve.setCustomValue(null);
        
        BigDecimal autoValue = reserve.calculateTotalValue();
        return reserveRepo.save(reserve);
    }

    public Reserve updateExtraGuestFee(Long reserveId, BigDecimal newFee) {
        Reserve reserve = reserveRepo.findById(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException(reserveId));
        
        reserve.setExtraGuestFee(newFee);
        
        BigDecimal newTotal = reserve.calculateTotalValue();
        
        return reserveRepo.save(reserve);
    }

    public Map<String, Object> getValueDetails(Long reserveId) {
        Reserve reserve = reserveRepo.findById(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException(reserveId));
        
        Map<String, Object> details = new HashMap<>();
        details.put("reserveId", reserve.getId());
        details.put("roomBaseValue", reserve.getInitialValue());
        details.put("numberOfDays", reserve.getNumberOfDays());
        details.put("numberOfGuests", reserve.getGuest().size());
        details.put("numberOfExtraGuests", reserve.getNumberOfExtraGuests());
        details.put("extraGuestFee", reserve.getExtraGuestFee());
        details.put("useCustomValue", reserve.getUseCustomValue());
        details.put("customValue", reserve.getCustomValue());
        details.put("calculatedTotal", reserve.calculateTotalValue());
        
        if (Boolean.FALSE.equals(reserve.getUseCustomValue())) {
            BigDecimal baseValue = reserve.getInitialValue() != null ? reserve.getInitialValue() : BigDecimal.ZERO;
            int numberOfDays = reserve.getNumberOfDays();
            int extraGuests = reserve.getNumberOfExtraGuests();
            
            BigDecimal dailyTotal = baseValue.multiply(BigDecimal.valueOf(numberOfDays));
            BigDecimal extraFees = reserve.getExtraGuestFee().multiply(BigDecimal.valueOf(extraGuests * numberOfDays));
            
            details.put("calculationBreakdown", Map.of(
                "dailyTotal", dailyTotal,
                "extraFees", extraFees,
                "formula", "(" + baseValue + " × " + numberOfDays + ") + (" + 
                        reserve.getExtraGuestFee() + " × " + extraGuests + " × " + numberOfDays + ")"
            ));
        }
        
        return details;
    }

    @Transactional
    public Reserve updateReserveDates(Long reserveId, Set<LocalDate> newDates) {
        Reserve reserve = reserveRepo.findById(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException(reserveId));

        validateReserveCanBeModified(reserve);

        for (LocalDate newDate : newDates) {
            validateNewDate(newDate);
            validateAvailability(reserve, newDate);
        }

        reserve.setReservedDays(newDates);

        if (Boolean.FALSE.equals(reserve.getUseCustomValue())) {
            BigDecimal newTotal = reserve.calculateTotalValue();
        }

        updateOccupations(reserve, newDates);

        return reserveRepo.save(reserve);
    }

    @Transactional
    public Reserve cancelReserve(Long reserveId) {
        Reserve reserve = reserveRepo.findById(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException(reserveId));

        if (reserve.getReserveStatus() == ReserveStatus.CANCELLED) {
            throw new IllegalStateException("Reserve is already cancelled");
        }

        if (!reserve.getCheckIn().isEmpty()) {
            throw new IllegalStateException("Cannot cancel reserve after check-in");
        }

        reserve.setReserveStatus(ReserveStatus.CANCELLED);
        Reserve cancelledReserve = reserveRepo.save(reserve);

        removeOccupations(reserve);

        logger.info("Reserve cancelled successfully: " + reserveId);
        return cancelledReserve;
    }

    private void removeOccupations(Reserve reserve) {
        List<RoomOccupation> roomOccupations = roomOccupationRepo.findByReserve(reserve);
        roomOccupationRepo.deleteAll(roomOccupations);

        List<BedOccupation> bedOccupations = bedOccupationRepo.findByReserve(reserve);
        bedOccupationRepo.deleteAll(bedOccupations);
    }

    @Transactional
    public Reserve cancelReserveByGuestAndDates(cancelReserveByGuestAndDatesRequest request) {
        var guest = guestRepo.findByName(request.guestName())
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + request.guestName()));

        Reserve reserve = reserveRepo.findAll().stream()
                .filter(r -> r.getGuest().contains(guest))
                .filter(r -> r.getReservedDays().equals(request.dates()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Reserve not found for guest: " + request.guestName() + " on dates: " + request.dates()));

        return cancelReserve(reserve.getId());
    }

    public Reserve addRoom(Long reserveId, Integer roomNumber) {
        Reserve reserve = reserveRepo.findById(reserveId)
            .orElseThrow(() -> new ResourceNotFoundException("Reserve not found with id: " + reserveId));

        Room room = roomRepo.findByNumber(roomNumber).orElseThrow(() -> new ResourceNotFoundException(roomNumber));    

        if (reserve.getReserveStatus() == ReserveStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a cancelled reserve");
        }

        reserve.getRooms().add(room);
        
        if (reserve.getRooms().size() == 1) {
            reserve.setInitialValue(room.getPrice());
            if (Boolean.FALSE.equals(reserve.getUseCustomValue())) {
                BigDecimal newTotal = reserve.calculateTotalValue();
            }
        }
        
        reserveRepo.save(reserve);

        room.getReservation().add(reserve);
    
        return reserve;
    }

    public Reserve removeRoom(Long reserveId, Integer roomNumber) {
        Reserve reserve = reserveRepo.findById(reserveId)
            .orElseThrow(() -> new ResourceNotFoundException("Reserve not found with id: " + reserveId));

        Room room = roomRepo.findByNumber(roomNumber).orElseThrow(() -> new ResourceNotFoundException(roomNumber));    

        if (reserve.getReserveStatus() == ReserveStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a cancelled reserve");
        }

        reserve.getRooms().remove(room);
        
        if (reserve.getRooms().isEmpty()) {
            reserve.setInitialValue(BigDecimal.ZERO);
            if (Boolean.FALSE.equals(reserve.getUseCustomValue())) {
                logger.info("Último quarto removido. Valor base zerado.");
            }
        }
        
        reserveRepo.save(reserve);

        room.getReservation().remove(reserve);
        roomRepo.save(room);
        return reserve;
    }

    @Transactional
    public Reserve addDate(Long reserveId, LocalDate newDate) {
        Reserve reserve = reserveRepo.findById(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserve not found with id: " + reserveId));

        validateReserveCanBeModified(reserve);

        validateNewDate(newDate);

        validateAvailability(reserve, newDate);

        Set<LocalDate> updatedDates = new HashSet<>(reserve.getReservedDays());
        
        if (updatedDates.contains(newDate)) {
            throw new IllegalStateException("Date " + newDate + " is already included in the reservation");
        }

        updatedDates.add(newDate);
        reserve.setReservedDays(updatedDates);

        if (Boolean.FALSE.equals(reserve.getUseCustomValue())) {
            BigDecimal newTotal = reserve.calculateTotalValue();
        }
        updateOccupationsForNewDate(reserve, newDate);

        Reserve updatedReserve = reserveRepo.save(reserve);
        return updatedReserve;
    }

    private void validateReserveCanBeModified(Reserve reserve) {
        if (reserve.getReserveStatus() == ReserveStatus.CANCELLED) {
            throw new IllegalStateException("Cannot modify a cancelled reserve");
        }

        // if (!reserve.getCheckIn().isEmpty()) {
        //     throw new IllegalStateException("Cannot modify reserve after check-in");
        // }

        // if (!reserve.getCheckOut().isEmpty()) {
        //     throw new IllegalStateException("Cannot modify reserve after check-out");
        // }
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
        
        if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
            validateExclusiveRoomAvailability(room, newDate, reserve.getId());
        } else if (room.isSharedRoom()) {
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

        if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
            updateRoomOccupationForNewDate(reserve, newDate);
        } else if (room.isSharedRoom()) {
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
                    return newRo;
                });
        roomOccupation.getOccupiedDays().add(newDate);
        roomOccupationRepo.save(roomOccupation);
    }

    private void updateBedOccupationForNewDate(Reserve reserve, LocalDate newDate) {
        Room room = reserve.getRooms().iterator().next();    
        BedOccupation bedOccupation = bedOccupationRepo.findByReserve(reserve)
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

    public Reserve removeDate(Long reserveId,  LocalDate date) {
        Reserve reserve = reserveRepo.findById(reserveId)
            .orElseThrow(() -> new ResourceNotFoundException("Reserve not found with id: " + reserveId));

        reserve.getReservedDays().remove(date);
                if (Boolean.FALSE.equals(reserve.getUseCustomValue())) {
            BigDecimal newTotal = reserve.calculateTotalValue();
            logger.info("📅 Data removida. Novo valor calculado: " + newTotal + 
                       " (Dias: " + reserve.getNumberOfDays() + ")");
        }
        
        reserveRepo.save(reserve);    
        return reserve;
    }
    
    @Transactional
    public Reserve addDates(Long reserveId, AddDatesRequest newDates) {
        Reserve reserve = reserveRepo.findById(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserve not found with id: " + reserveId));

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

        if (Boolean.FALSE.equals(reserve.getUseCustomValue())) {
            BigDecimal newTotal = reserve.calculateTotalValue();
        }

        for (LocalDate newDate : newDates.dates()) {
            updateOccupationsForNewDate(reserve, newDate);
        }

        Reserve updatedReserve = reserveRepo.save(reserve);
        logger.info("Dates added successfully to reserve " + reserveId + ": " + newDates);
        
        return updatedReserve;
    }

    private void validateNewDates(Set<LocalDate> newDates) {
        LocalDate today = LocalDate.now();
        for (LocalDate date : newDates) {
            if (date.isBefore(today)) {
                throw new IllegalArgumentException("Cannot reserve dates in the past: " + date);
            }
        }
    }

    private void updateOccupations(Reserve reserve, Set<LocalDate> newDates) {
        removeOccupations(reserve);
        Room room = reserve.getRooms().iterator().next();
        if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
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
            roomOccupationRepo.save(ro);
        }

        if (room.isSharedRoom()) {
            Bed availableBed = bedRepo.findAll().stream()
                    .filter(b -> b.getRoom().equals(room))
                    .filter(b -> bedOccupationRepo.findConflicts(b, newDates).isEmpty())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No available beds in shared room " + room.getNumber() + " for the new dates"));
            BedOccupation bo = new BedOccupation();
            bo.setBed(availableBed);
            bo.setReserve(reserve);
            bo.getOccupiedDays().addAll(newDates);
            bedOccupationRepo.save(bo);
        }
    }

    public Reserve checkIn(Long id) {        
        Reserve reserva = reserveRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
        reserva.getCheckIn().add(LocalDateTime.now());

        var rooms = reserva.getRooms();
        for (Room room : rooms) {
            if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
                room.setRoomStatus(RoomStatus.OCCUPIED);
            } else if (room.isSharedRoom()) {
                BedOccupation bedOccupation = bedOccupationRepo.findByReserveAndRoom(reserva, room)
                        .orElseThrow(() -> new IllegalStateException("BedOccupation not found for reserve " + id + " in room " + room.getNumber()));
                Bed bed = bedOccupation.getBed();
                bed.setBedStatus(BedStatus.OCCUPIED);
                bedRepo.save(bed);

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
        Reserve reserve = reserveRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
        reserve.getCheckOut().add(LocalDateTime.now());

        var rooms = reserve.getRooms();
        for (Room room : rooms) {
            if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
                room.setRoomStatus(RoomStatus.VAGUE);
                logger.info("✅ Quarto exclusivo " + room.getNumber() + " liberado para VAGUE");
                
            } else if (room.isSharedRoom()) {
                Bed reservedBed = findBedForReserveInRoom(reserve, room);
                if (reservedBed != null) {
                    reservedBed.setBedStatus(BedStatus.VAGUE);
                    bedRepo.save(reservedBed);
                    logger.info("✅ Cama " + reservedBed.getId() + " no quarto compartilhado " + room.getNumber() + " liberada para VAGUE");
                    updateSharedRoomStatus(room);
                }
            }
        }
        roomRepo.saveAll(rooms);
        
        return reserveRepo.save(reserve);
    }

    private Bed findBedForReserveInRoom(Reserve reserve, Room room) {
        try {
            List<BedOccupation> bedOccupations = bedOccupationRepo.findByReserve(reserve);
            
            return bedOccupations.stream()
                    .filter(bo -> bo.getBed().getRoom().equals(room))
                    .map(BedOccupation::getBed)
                    .findFirst()
                    .orElse(null);
                    
        } catch (Exception e) {
            logger.warning("❌ Erro ao buscar cama para reserva " + reserve.getId() + " no quarto " + room.getNumber());
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
                logger.info("🏨 Quarto compartilhado " + room.getNumber() + " agora está OCCUPIED (todas as " + totalBeds + " camas ocupadas)");
            }
        } else if (occupiedBeds == 0) {
            if (room.getRoomStatus() != RoomStatus.VAGUE) {
                room.setRoomStatus(RoomStatus.VAGUE);
                logger.info("🏨 Quarto compartilhado " + room.getNumber() + " agora está VAGUE (todas as " + totalBeds + " camas vagas)");
            }
        } else {
            logger.info("🔸 Quarto compartilhado " + room.getNumber() + " tem " + occupiedBeds + "/" + totalBeds + " camas ocupadas");
        }
        
        roomRepo.save(room);
    }

    public Map<String, Object> checkAvailability(Integer roomNumber, LocalDate checkIn, LocalDate checkOut) {
    validateDates(checkIn, checkOut);
    
    Set<LocalDate> requestedDates = getDatesBetween(checkIn, checkOut);
    
    Room room = roomRepo.findByNumber(roomNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado: " + roomNumber));
    boolean isAvailable;
    String message;
    String roomTypeDescription = room.getRoomTypeDescription();
    
    if (room.isAnyExclusiveType()) {
        boolean hasConflict = roomOccupationRepo.existsConflictForRoomAndDates(room, requestedDates);
        
        isAvailable = !hasConflict;
        message = hasConflict ? 
            String.format("%s não disponível para as datas solicitadas", roomTypeDescription) : 
            String.format("%s disponível", roomTypeDescription);
            
    } else if (room.isSharedRoom()) {
        long availableBeds = bedRepo.findByRoomAndBedStatus(room, BedStatus.VAGUE).stream()
                .filter(bed -> bedOccupationRepo.isBedAvailableForDates(bed, requestedDates))
                .count();
        
        isAvailable = availableBeds > 0;
        message = isAvailable ? 
            String.format("%d cama(s) disponível(is) no %s", availableBeds, roomTypeDescription.toLowerCase()) :
            String.format("Nenhuma cama disponível no %s", roomTypeDescription.toLowerCase());
            
    } else {
        isAvailable = false;
        message = "Tipo de quarto não reconhecido";
    }
    
    // Verificar se o quarto está em manutenção
    // if (isAvailable && room.getRoomStatus() == RoomStatus.MAINTENANCE) {
    //     isAvailable = false;
    //     message = String.format("%s em manutenção", roomTypeDescription);
    // }
    
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
        try {
            Reserve reserve = reserveRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(id));            
            List<RoomOccupation> roomOccupations = roomOccupationRepo.findByReserve(reserve);
            if (!roomOccupations.isEmpty()) {
                logger.info("📤 Removendo " + roomOccupations.size() + " room occupations");
                roomOccupationRepo.deleteAll(roomOccupations);
            }

            List<BedOccupation> bedOccupations = bedOccupationRepo.findByReserve(reserve);
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
            
            logger.info("✅ Reserva #" + id + " excluída com sucesso");

        } catch (Exception e) {
            throw new RuntimeException("Falha ao excluir reserva: " + e.getMessage(), e);
        }
    }
    

    public Reserve reserveUpdateExtra(Long id, UpdateDataReserveDTO entity) {
        var obj = reserveRepo.getReferenceById(id);
        updateData(obj, entity);
        return reserveRepo.save(obj);
    }

    private void updateData(Reserve entity, UpdateDataReserveDTO obj) {
        if(obj.customValue() != null) {
            entity.setCustomValue(obj.customValue());
        }
        if (obj.extraGuestFee() != null) {
            entity.setExtraGuestFee(obj.extraGuestFee());
        }
        if (obj.useCustomValue() != null) {
            entity.setUseCustomValue(obj.useCustomValue());
        }
        if (obj.initialValue() != null) {
            entity.setInitialValue(obj.initialValue());
        }
    }

}