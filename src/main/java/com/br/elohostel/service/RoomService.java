package com.br.elohostel.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.elohostel.exceptions.DatabaseException;
import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Bed;
import com.br.elohostel.model.BedOccupation;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.Tenant;
import com.br.elohostel.model.enums.BedStatus;
import com.br.elohostel.repository.BedOccupationRepository;
import com.br.elohostel.repository.BedRepository;
import com.br.elohostel.repository.RoomRepository;
import com.br.elohostel.repository.TenantRepository;
import com.br.elohostel.util.TenantContext;

@Service
public class RoomService {

    private final RoomRepository repo;
    private final BedRepository bedRepo;
    private final BedOccupationRepository bedOccupationRepo;
    private final TenantRepository tenantRepo;

    public RoomService(RoomRepository repo, BedRepository bedRepo, 
                      BedOccupationRepository bedOccupationRepo,
                      TenantRepository tenantRepo) {
        this.repo = repo;
        this.bedRepo = bedRepo;
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

    public Room findNumber(Integer number) {
        String tenantKey = getCurrentTenantKey();
        return repo.findByNumberAndTenant_TenantKey(number, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado: " + number));
    }

    public List<Room> findAll() {
        return repo.findByTenant_TenantKey(getCurrentTenantKey());
    }

    public Room findById(Long id) {
        String tenantKey = getCurrentTenantKey();
        return repo.findByIdAndTenant_TenantKey(id, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    @Transactional
    public Room insert(Room entity) {
        entity.setTenant(getCurrentTenant());
        return repo.save(entity);
    }

    @Transactional
    public Room insertBed(Long id) {
        Room room = findById(id);
        Bed newBed = new Bed(BedStatus.VAGUE, room);
        newBed.setTenant(room.getTenant());
        bedRepo.save(newBed);
        room.getBeds().add(newBed);
        return repo.save(room);
    }

    @Transactional
    public Room removeBed(Long roomId) {
        Room room = findById(roomId);
        String tenantKey = getCurrentTenantKey();

        if (room.getBeds().isEmpty()) {
            throw new IllegalStateException("O quarto não possui camas para remover");
        }

        Optional<Bed> availableBedToRemove = room.getBeds().stream()
                .filter(bed -> bed.getBedStatus() == BedStatus.VAGUE)
                .findFirst();

        if (availableBedToRemove.isEmpty()) {
            throw new IllegalStateException("Não há camas vagas disponíveis para remoção no quarto " + room.getNumber());
        }

        Bed bedToRemove = availableBedToRemove.get();
        
        if (!bedToRemove.getTenant().getTenantKey().equals(tenantKey)) {
            throw new ResourceNotFoundException("Cama não encontrada no tenant atual");
        }

        List<BedOccupation> bedOccupations = bedOccupationRepo.findByBedAndTenant_TenantKey(bedToRemove, tenantKey);
        
        // Opcional: verificar ocupações futuras
        // boolean hasFutureOccupations = bedOccupations.stream()
        //         .anyMatch(bo -> bo.getOccupiedDays().stream()
        //                 .anyMatch(date -> !date.isBefore(java.time.LocalDate.now())));
        // if (hasFutureOccupations) {
        //     throw new IllegalStateException("Não é possível remover a cama " + bedToRemove.getId() + 
        //             " pois ela tem reservas futuras");
        // }

        room.getBeds().remove(bedToRemove);
        repo.save(room);

        if (!bedOccupations.isEmpty()) {
            bedOccupationRepo.deleteAll(bedOccupations);
        }

        bedRepo.delete(bedToRemove);

        return room;
    }

    @Transactional
    public void deleteById(Long id) {
        Room room = findById(id);
        try {
            repo.delete(room);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    @Transactional
    public Room update(Long id, Room obj) {
        Room entity = findById(id); 
        updateData(entity, obj);
        return repo.save(entity);
    }

    private void updateData(Room entity, Room obj) {
        if (obj.getNumber() != null) {
            entity.setNumber(obj.getNumber());
        }
        if (obj.getRoomStatus() != null) {
            entity.setRoomStatus(obj.getRoomStatus());
        }
        if (obj.getRoomType() != null) {
            entity.setRoomType(obj.getRoomType());
        }
        if (obj.getPrice() != null) {
            entity.setPrice(obj.getPrice());
        }
    }
}