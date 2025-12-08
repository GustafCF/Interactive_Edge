package com.br.elohostel.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.br.elohostel.model.Bed;
import com.br.elohostel.model.enums.BedStatus;
import com.br.elohostel.repository.BedRepository;

@Service
public class BedService {

    private final BedRepository bedRepo;

    public BedService(BedRepository bedRepo) {
        this.bedRepo = bedRepo;
    }

    public List<Bed> findAll() {
        return bedRepo.findAll();
    }

    public List<Bed> findByBedStatusVague(){
        return bedRepo.findByBedStatus(BedStatus.VAGUE);
    }

    public List<Bed> findByBedStatusOccupied() {
        return bedRepo.findByBedStatus(BedStatus.OCCUPIED);
    }

    public List<Bed> findByRoom(Long roomId) {
        return bedRepo.findByRoomId(roomId);
    }

    public List<Bed> findAvailableBedsByRoomVague(Long roomId) {
        return bedRepo.findByBedStatusAndRoomId(BedStatus.VAGUE, roomId);
    }
}