package com.br.elohostel.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.br.elohostel.exceptions.ResourceNotFoundException;
import com.br.elohostel.model.Role;
import com.br.elohostel.model.User;
import com.br.elohostel.model.dtos.CreateUserDto;
import com.br.elohostel.model.enums.RoleStatus;
import com.br.elohostel.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public User findById(Long id) {
        Optional<User> obj = userRepo.findById(id);
        return obj.orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public User findByName(String name){
        Optional<User> obj = userRepo.findByName(name);
        return obj.orElseThrow(() -> new ResourceNotFoundException(name));
    }

    public User findByEmail(String email){
        Optional<User> obj = userRepo.findByEmail(email);
        return obj.orElseThrow(() -> new ResourceNotFoundException(email));
    }

    public User insert(CreateUserDto dto) {
        User obj = new User();
        obj.setName(dto.name());
        obj.setUsername(dto.username());
        obj.setEmail(dto.email());
        obj.setPassword(dto.password());
        obj.setPhone(dto.phone());
        obj.getRoles().add(new Role(RoleStatus.BASIC.name(), "Basic role", RoleStatus.BASIC));
        return userRepo.save(obj);
    }

    public void delete(Long id) {
        userRepo.deleteById(id);
    }

    public User update(Long id, User u1) {
        User obj = userRepo.getReferenceById(id);
        updateData(obj, u1);
        return userRepo.save(obj);
    } 

    private void updateData(User u1, User u2) {
        if (!u2.getName().isBlank()){
            u1.setName(u2.getName());
        }
        if(!u2.getUsername().isBlank()) {
            u1.setUsername(u2.getUsername());
        }
        if(!u2.getEmail().isBlank()) {
            u1.setEmail(u2.getEmail());
        }
        if(!u2.getPassword().isBlank()) {
            u1.setPassword(u2.getPassword());
        }
        if (!u2.getPhone().isBlank()) {
            u1.setPhone(u2.getPhone());
        }
    }
}