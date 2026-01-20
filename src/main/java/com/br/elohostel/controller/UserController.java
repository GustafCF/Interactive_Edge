package com.br.elohostel.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.br.elohostel.model.User;
import com.br.elohostel.model.dtos.CreateUserDto;
import com.br.elohostel.service.UserService;

@RestController
@RequestMapping("/us")
public class UserController {

    private final UserService service;
    
    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> findAll() {
        List<User> list = service.findAll();
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/fd/id/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        var user = service.findById(id);
        return ResponseEntity.ok().body(user);
    }

    @GetMapping("/fd/nm/{name}")
    public ResponseEntity<User> findByName(@PathVariable String name) {
        var user = service.findByName(name);
        return ResponseEntity.ok().body(user);
    }

    @GetMapping("/fd/em/{email}")
    public ResponseEntity<User> findByEmail(@PathVariable String email) {
        var user = service.findByEmail(email);
        return ResponseEntity.ok().body(user);
    }

    @PostMapping("/is")
    public ResponseEntity<User> insert(@RequestBody CreateUserDto dto) {
        var user = service.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(uri).body(user);
    }

    @DeleteMapping("/dl/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/up/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User obj) {
        var user = service.update(id, obj);
        return ResponseEntity.ok().body(user);
    }
}
