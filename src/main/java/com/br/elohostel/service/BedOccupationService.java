package com.br.elohostel.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.br.elohostel.model.Bed;
import com.br.elohostel.model.BedAvailabilityInfo;
import com.br.elohostel.model.BedOccupation;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.enums.BedStatus;
import com.br.elohostel.repository.BedOccupationRepository;
import com.br.elohostel.repository.BedRepository;
import com.br.elohostel.repository.RoomRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class BedOccupationService {

    private final BedOccupationRepository bedOccupationRepository;
    private final BedRepository bedRepository;
    private final RoomRepository roomRepository;

    public BedOccupationService(BedOccupationRepository bedOccupationRepository,
                              BedRepository bedRepository,
                              RoomRepository roomRepository) {
        this.bedOccupationRepository = bedOccupationRepository;
        this.bedRepository = bedRepository;
        this.roomRepository = roomRepository;
    }

    public List<Bed> findAvailableBedsInRoom(Room room, Set<LocalDate> dates) {
        return bedRepository.findBedsInRoomByStatus(room.getId(), BedStatus.AVAILABLE, dates);
    }

    public List<Bed> findAvailableBedsInRoomAlternative(Room room, Set<LocalDate> dates) {
        List<Bed> unoccupiedBeds = bedRepository.findUnoccupiedBedsInRoom(room.getId(), dates);
        return unoccupiedBeds.stream()
                .filter(bed -> bed.getBedStatus() == BedStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    public List<Bed> findAvailableAndUnoccupiedBeds(Room room, Set<LocalDate> dates) {
        return bedRepository.findAvailableAndUnoccupiedBeds(room.getId(), dates);
    }

    public void updateBedOccupations(Reserve reserve) {
        removeExistingBedOccupations(reserve);
        for (Room room : reserve.getRooms()) {
            allocateBedsForRoom(room, reserve);
        }
    }

    private void allocateBedsForRoom(Room room, Reserve reserve) {
        Set<LocalDate> reservedDays = reserve.getReservedDays();
        List<Bed> availableBeds = findAvailableBedsInRoom(room, reservedDays);
        for (Bed bed : availableBeds) {
            if (isBedAvailableForDates(bed, reservedDays)) {
                createBedOccupation(bed, reserve, reservedDays);
            }
        }
    }

    public boolean isBedAvailableForDates(Bed bed, Set<LocalDate> dates) {
        return !bedOccupationRepository.existsByBedIdAndOccupiedDaysIn(bed.getId(), dates);
    }

    public BedOccupation createBedOccupation(Bed bed, Reserve reserve, Set<LocalDate> occupiedDays) {
        BedOccupation occupation = new BedOccupation();
        occupation.setBed(bed);
        occupation.setReserve(reserve);
        occupation.getOccupiedDays().addAll(new HashSet<>(occupiedDays));
        bed.setBedStatus(BedStatus.OCCUPIED);
        bedRepository.save(bed);
        
        return bedOccupationRepository.save(occupation);
    }

    public void removeExistingBedOccupations(Reserve reserve) {
        List<BedOccupation> existingOccupations = bedOccupationRepository.findByReserve(reserve);  
        for (BedOccupation occupation : existingOccupations) {
            Bed bed = occupation.getBed();
            bed.setBedStatus(BedStatus.AVAILABLE);
            bedRepository.save(bed);
            bedOccupationRepository.delete(occupation);
        }
    }

    public List<BedAvailabilityInfo> getBedAvailabilityByRoomAndDates(Set<LocalDate> dates) {
        List<Room> allRooms = roomRepository.findAll();
        List<BedAvailabilityInfo> availabilityInfo = new ArrayList<>();
        
        for (Room room : allRooms) {
            Integer totalBeds = room.getBeds().size();
            Integer availableBeds = countAvailableBedsInRoom(room, dates);
            
            BedAvailabilityInfo info = new BedAvailabilityInfo(
                room.getNumber(),
                totalBeds,
                availableBeds,
                dates
            );
            availabilityInfo.add(info);
        }
        
        return availabilityInfo;
    }

    public Integer countAvailableBedsInRoom(Room room, Set<LocalDate> dates) {
        List<Bed> availableBeds = findAvailableBedsInRoom(room, dates);
        return availableBeds.size();
    }

    public List<BedOccupation> findOccupationsInPeriod(LocalDate startDate, LocalDate endDate) {
        return bedOccupationRepository.findByOccupiedDaysBetween(startDate, endDate);
    }

    public void releaseBedsByCheckout(Date checkOutDate) {
        LocalDate localCheckOut = checkOutDate.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        List<BedOccupation> occupationsToRelease = bedOccupationRepository
            .findByOccupiedDaysContaining(localCheckOut);
        for (BedOccupation occupation : occupationsToRelease) {
            Bed bed = occupation.getBed();
            bed.setBedStatus(BedStatus.AVAILABLE);
            bedRepository.save(bed);
            occupation.getOccupiedDays().remove(localCheckOut);
            if (occupation.getOccupiedDays().isEmpty()) {
                bedOccupationRepository.delete(occupation);
            } else {
                bedOccupationRepository.save(occupation);
            }
        }
    }

    public Map<String, Object> getBedOccupationStatistics(LocalDate startDate, LocalDate endDate) {
        List<BedOccupation> occupations = findOccupationsInPeriod(startDate, endDate);
        Long totalBeds = bedRepository.count();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBeds", totalBeds);
        stats.put("occupiedBeds", occupations.size());
        stats.put("occupationRate", totalBeds > 0 ? 
            (double) occupations.size() / totalBeds * 100 : 0);
        stats.put("period", startDate + " to " + endDate);
        return stats;
    }
}