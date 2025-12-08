package com.br.elohostel.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Guest;
import com.br.elohostel.repository.GuestRepository;

@Service
public class GuestService {

    private final GuestRepository repo;

    public GuestService(GuestRepository repo) {
        this.repo = repo;
    }

    public List<Guest> findAll() {
        return repo.findAll();
    }

    public Guest finById(Long id) {
        Optional<Guest> guest = repo.findById(id);
        return guest.orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Guest findByName(String name) {
        Optional<Guest> guest = repo.findByName(name);
        return guest.orElseThrow(() -> new ResourceNotFoundException(name));
    }

    public Guest insert(Guest insert) {
        return repo.save(insert);
    }

    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    public Guest update(Long id, Guest entity) {
        var obj = repo.getReferenceById(id);
        updateData(obj, entity);
        return repo.save(obj);
    }

    private void updateData(Guest entity, Guest obj) {
        if(!obj.getName().isBlank()) {
            entity.setName(obj.getName());
        }
        if(!obj.getRg().isBlank()) {
            entity.setRg(obj.getRg());
        }
        if(!obj.getPhone().isBlank()) {
            entity.setPhone(obj.getPhone());
        }
        if(!obj.getEmail().isBlank()) {
            entity.setEmail(obj.getEmail());
        }
    }
}