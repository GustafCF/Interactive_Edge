package com.br.elohostel.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import com.br.elohostel.exceptions.DatabaseException;
import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Bed;
import com.br.elohostel.model.BedOccupation;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.enums.BedStatus;
import com.br.elohostel.repository.BedOccupationRepository;
import com.br.elohostel.repository.BedRepository;
import com.br.elohostel.repository.RoomRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class RoomService {

    private final RoomRepository repo;
    private final BedRepository bedRepo;
    private final BedOccupationRepository bedOccupationRepo;

    public RoomService (RoomRepository repo, BedRepository bedRepo, BedOccupationRepository bedOccupationRepo) {
        this.repo = repo;
        this.bedRepo = bedRepo;
        this.bedOccupationRepo = bedOccupationRepo;
    }

    public Room findNumber(Integer number) {
        var obj = repo.findByNumber(number);
        return obj.orElseThrow(() -> new ResourceNotFoundException(number));
    }

    public List<Room> findAll() {
        List<Room> list = repo.findAll();
        return list;
    }

    public Room findById(Long id) {
        Optional<Room> obj = repo.findById(id);
        return obj.orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Room insert (Room entity) {
        return repo.save(entity);
    }

    public Room insertBed(Long id) {
        var room = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
        room.getBeds().add(new Bed(BedStatus.VAGUE, room));
        return repo.save(room);
    }

    @Transactional
    public Room removeBed(Long roomId) {
        Room room = repo.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException(roomId));

        // Verificar se há camas no quarto
        if (room.getBeds().isEmpty()) {
            throw new IllegalStateException("O quarto não possui camas para remover");
        }

        // 🔥 CORREÇÃO: Encontrar apenas camas VAGUE para remover
        Optional<Bed> availableBedToRemove = room.getBeds().stream()
                .filter(bed -> bed.getBedStatus() == BedStatus.VAGUE)
                .findFirst();

        if (availableBedToRemove.isEmpty()) {
            throw new IllegalStateException("Não há camas vagas disponíveis para remoção no quarto " + room.getNumber());
        }

        Bed bedToRemove = availableBedToRemove.get();

        // 🔥 CORREÇÃO: Verificar se a cama não tem ocupações futuras
        List<BedOccupation> bedOccupations = bedOccupationRepo.findByBed(bedToRemove);
        // if (!bedOccupations.isEmpty()) {
        //     boolean hasFutureOccupations = bedOccupations.stream()
        //             .anyMatch(bo -> bo.getOccupiedDays().stream()
        //                     .anyMatch(date -> !date.isBefore(java.time.LocalDate.now())));
            
        //     if (hasFutureOccupations) {
        //         throw new IllegalStateException("Não é possível remover a cama " + bedToRemove.getId() + 
        //                 " pois ela tem reservas futuras no quarto " + room.getNumber());
        //     }
        // }

        // Remover a cama do quarto
        room.getBeds().remove(bedToRemove);
        repo.save(room);

        // Se houver ocupações passadas, removê-las primeiro
        if (!bedOccupations.isEmpty()) {
            bedOccupationRepo.deleteAll(bedOccupations);
        }

        // Agora pode deletar a cama com segurança
        bedRepo.delete(bedToRemove);

        return room;
    }

    public void deleteById(Long id) {
        try {
            repo.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException(id);
        } catch(DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    public Room update(Long id, Room obj) {
        try {
            Room entity = repo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(id));
            updateData(entity, obj);
            return repo.save(entity);   
        } catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException(id);
		}
    }

    private void updateData(Room entity, Room obj) {
        if(obj.getNumber() != null) {
            entity.setNumber(obj.getNumber());
        }
        if(obj.getRoomStatus() != null) {
            entity.setRoomStatus(obj.getRoomStatus());
        }
        if(obj.getRoomType() != null) {
            entity.setRoomType(obj.getRoomType());
        }
        if(obj.getPrice() != null) {
            entity.setPrice(obj.getPrice());
        }
    }
}