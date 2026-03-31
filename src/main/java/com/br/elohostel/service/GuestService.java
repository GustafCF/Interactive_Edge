package com.br.elohostel.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Guest;
import com.br.elohostel.model.Tenant;
import com.br.elohostel.repository.GuestRepository;
import com.br.elohostel.repository.TenantRepository;
import com.br.elohostel.util.TenantContext;

@Service
public class GuestService {

    private final GuestRepository guestRepo;
    private final TenantRepository tenantRepo;

    public GuestService(GuestRepository guestRepo, TenantRepository tenantRepo) {
        this.guestRepo = guestRepo;
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

    public List<Guest> findAll() {
        return guestRepo.findByTenant_TenantKey(getCurrentTenantKey());
    }

    public Guest findById(Long id) {
        String tenantKey = getCurrentTenantKey();
        return guestRepo.findByIdAndTenant_TenantKey(id, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Guest findByName(String name) {
        String tenantKey = getCurrentTenantKey();
        return guestRepo.findByNameAndTenant_TenantKey(name, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException(name));
    }
    
    public Guest findByRg(String rg) {
        String tenantKey = getCurrentTenantKey();
        return guestRepo.findByRgAndTenant_TenantKey(rg, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException("RG não encontrado: " + rg));
    }
    
    public Guest findByEmail(String email) {
        String tenantKey = getCurrentTenantKey();
        return guestRepo.findByEmailAndTenant_TenantKey(email, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException("Email não encontrado: " + email));
    }
    
    public List<Guest> findByNameContaining(String name) {
        String tenantKey = getCurrentTenantKey();
        return guestRepo.findByNameContainingIgnoreCaseAndTenant_TenantKey(name, tenantKey);
    }

        public boolean existsByRg(String rg) {
        String tenantKey = getCurrentTenantKey();
        return guestRepo.existsByRgAndTenant_TenantKey(rg, tenantKey);
    }

    public Guest insert(Guest guest) {
        guest.setTenant(getCurrentTenant());
        return guestRepo.save(guest);
    }

    public void deleteById(Long id) {
        Guest guest = findById(id);
        guestRepo.delete(guest);
    }

    public Guest update(Long id, Guest guestData) {
        Guest existingGuest = findById(id);
        updateData(existingGuest, guestData);
        return guestRepo.save(existingGuest);
    }

    private void updateData(Guest existingGuest, Guest newData) {
        if (newData.getName() != null && !newData.getName().isBlank()) {
            existingGuest.setName(newData.getName());
        }
        if (newData.getRg() != null && !newData.getRg().isBlank()) {
            existingGuest.setRg(newData.getRg());
        }
        if (newData.getPhone() != null && !newData.getPhone().isBlank()) {
            existingGuest.setPhone(newData.getPhone());
        }
        if (newData.getEmail() != null && !newData.getEmail().isBlank()) {
            existingGuest.setEmail(newData.getEmail());
        }
    }
    

}