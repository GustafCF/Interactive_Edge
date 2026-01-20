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

import com.br.elohostel.model.Guest;
import com.br.elohostel.service.GuestService;

@RestController
@RequestMapping("/guest")
public class GuestController {

    private final GuestService service;

    public GuestController(GuestService service) {
        this.service = service;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Guest>> findAll(){
        List<Guest> list = service.findAll();
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Guest> findById(@PathVariable Long id) {
        var obj = service.finById(id);
        return ResponseEntity.ok().body(obj);
    }

    @GetMapping("/find/{name}")
    public ResponseEntity<Guest> findByName(@PathVariable String name) {
        var obj = service.findByName(name);
        return ResponseEntity.ok().body(obj);
    }

    @PostMapping("/insert")
    public ResponseEntity<Guest> insert(@RequestBody Guest guest) {
        var obj = service.insert(guest);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("{id}").buildAndExpand(obj.getId()).toUri();
        return ResponseEntity.created(uri).body(obj);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Guest> update(@PathVariable Long id, @RequestBody Guest guest) {
        var obj = service.update(id, guest);
        return ResponseEntity.ok().body(obj);
    }
}