package com.br.elohostel.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Role;
import com.br.elohostel.model.Tenant;
import com.br.elohostel.model.User;
import com.br.elohostel.model.dtos.CreateUserDto;
import com.br.elohostel.model.enums.SubscriptionStatus;
import com.br.elohostel.repository.RoleRepository;
import com.br.elohostel.repository.TenantRepository;
import com.br.elohostel.repository.UserRepository;
import com.br.elohostel.util.TenantContext;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RoleRepository roleRepo;

    public UserService(UserRepository userRepo, TenantRepository tenantRepo, BCryptPasswordEncoder passwordEncoder, RoleRepository roleRepo) {
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.passwordEncoder = passwordEncoder;
        this.roleRepo = roleRepo;
    }

    public List<User> findAll() {
        String tenantKey = TenantContext.getCurrentTenant();
        return userRepo.findByTenant_TenantKey(tenantKey);
    }

    public User findById(Long id) {
        String tenantKey = TenantContext.getCurrentTenant();
        return userRepo.findByIdAndTenant_TenantKey(id, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public User findByName(String name) {
        String tenantKey = TenantContext.getCurrentTenant();
        return userRepo.findByNameAndTenant_TenantKey(name, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException(name));
    }

    public User findByEmail(String email) {
        String tenantKey = TenantContext.getCurrentTenant();
        return userRepo.findByEmailAndTenant_TenantKey(email, tenantKey)
                .orElseThrow(() -> new ResourceNotFoundException(email));
    }

    @Transactional
    public User register(CreateUserDto dto) {
        Optional<Role> role = roleRepo.findById(3L);
        String uuid = UUID.randomUUID().toString();
        String tenantKey = "TNT_" + uuid + "_" + dto.email(); 
        String tenantName = "TNT_US" + dto.name();
        Tenant t = new Tenant(tenantKey, tenantName, SubscriptionStatus.TRIAL, LocalDateTime.now());
        tenantRepo.save(t);
        User u = new User();
        u.setName(dto.name());
        u.setEmail(dto.email());
        u.setPassword(passwordEncoder.encode(dto.password()));
        u.setPhone(dto.phone());
        u.getRoles().add(role.get());
        u.setTenant(t);
        userRepo.save(u);
        return u;
    }

    public User insert(CreateUserDto dto) {
        String tenantKey = TenantContext.getCurrentTenant();
        Tenant tenant = tenantRepo.findByTenantKey(tenantKey);
        if (tenant == null) {
            throw new RuntimeException("Tenant não encontrado: " + tenantKey);
        }
        Role role = roleRepo.findByName(dto.roleName());

        User obj = new User();
        obj.setName(dto.name());
        obj.setEmail(dto.email());
        obj.setPassword(passwordEncoder.encode(dto.password()));
        obj.setPhone(dto.phone());
        obj.getRoles().add(role);
        obj.setTenant(tenant);
        return userRepo.save(obj);
    }

    public void delete(Long id) {
        User user = findById(id);
        userRepo.delete(user);
    }

    public User updateRole(String user, String nameRole) {
        String tenantKey = TenantContext.getCurrentTenant();
        Optional<User> us = userRepo.findByNameAndTenant_TenantKey(user, tenantKey);
        Role role = roleRepo.findByName(nameRole);
        us.get().getRoles().clear();
        us.get().getRoles().add(role);
        return userRepo.save(us.get());
    }

    public User update(Long id, User userData) {
        User existing = findById(id);
        updateData(existing, userData);
        return userRepo.save(existing);
    }

    private void updateData(User existing, User newData) {
        if (newData.getName() != null && !newData.getName().isBlank()) {
            existing.setName(newData.getName());
        }
        if (newData.getEmail() != null && !newData.getEmail().isBlank()) {
            existing.setEmail(newData.getEmail());
        }
        if (newData.getPassword() != null && !newData.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(newData.getPassword()));
        }
        if (newData.getPhone() != null && !newData.getPhone().isBlank()) {
            existing.setPhone(newData.getPhone());
        }
        
        // NOVOS CAMPOS - Adicione esta parte!
        if (newData.getEstablishmentName() != null) {
            existing.setEstablishmentName(newData.getEstablishmentName());
        }
        if (newData.getEstablishmentAddress() != null) {
            existing.setEstablishmentAddress(newData.getEstablishmentAddress());
        }
        if (newData.getEstablishmentPhone() != null) {
            existing.setEstablishmentPhone(newData.getEstablishmentPhone());
        }
        if (newData.getEstablishmentResponsible() != null) {
            existing.setEstablishmentResponsible(newData.getEstablishmentResponsible());
        }
        if (newData.getEstablishmentLogo() != null) {
            existing.setEstablishmentLogo(newData.getEstablishmentLogo());
        }
        if (newData.getEstablishmentWelcomeMessage() != null) {
            existing.setEstablishmentWelcomeMessage(newData.getEstablishmentWelcomeMessage());
        }
    }
}