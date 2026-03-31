package com.br.elohostel.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.br.elohostel.model.Bed;
import com.br.elohostel.model.Tenant;
import com.br.elohostel.model.enums.BedStatus;
import com.br.elohostel.repository.BedRepository;
import com.br.elohostel.repository.TenantRepository;
import com.br.elohostel.util.TenantContext;

@Service
public class BedService {

    private final BedRepository bedRepo;
    private final TenantRepository tenantRepo;

    public BedService(BedRepository bedRepo, TenantRepository tenantRepo) {
        this.bedRepo = bedRepo;
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

    public List<Bed> findAll() {
        return bedRepo.findByBedStatusAndTenant_TenantKey(null, getCurrentTenantKey());
    }

    public List<Bed> findByBedStatusVague() {
        return bedRepo.findByBedStatusAndTenant_TenantKey(BedStatus.VAGUE, getCurrentTenantKey());
    }

    public List<Bed> findByBedStatusOccupied() {
        return bedRepo.findByBedStatusAndTenant_TenantKey(BedStatus.OCCUPIED, getCurrentTenantKey());
    }

    public List<Bed> findByRoom(Long roomId) {
        return bedRepo.findByRoomIdAndTenant_TenantKey(roomId, getCurrentTenantKey());
    }

    public List<Bed> findAvailableBedsByRoomVague(Long roomId) {
        return bedRepo.findByBedStatusAndRoomIdAndTenant_TenantKey(BedStatus.VAGUE, roomId, getCurrentTenantKey());
    }

    public Bed save(Bed bed) {
        bed.setTenant(getCurrentTenant());
        return bedRepo.save(bed);
    }

    public Bed findById(Long id) {
        return bedRepo.findById(id)
                .filter(bed -> bed.getTenant().getTenantKey().equals(getCurrentTenantKey()))
                .orElseThrow(() -> new RuntimeException("Cama não encontrada: " + id));
    }

    public void delete(Long id) {
        Bed bed = findById(id);
        bedRepo.delete(bed);
    }
}