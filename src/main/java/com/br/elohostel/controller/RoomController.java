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

import com.br.elohostel.model.Room;
import com.br.elohostel.service.RoomService;

@RestController
@RequestMapping("/room")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @GetMapping("/number")
    public ResponseEntity<Room> findByNumber(@RequestBody Integer number) {
        Room obj = service.findNumber(number);
        return ResponseEntity.ok().body(obj);
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Room> findById(@PathVariable Long id) {
        Room obj = service.findById(id);
        return ResponseEntity.ok().body(obj);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Room>> findAll() {
        List<Room> list = service.findAll();
        return ResponseEntity.ok().body(list);
    }

    @PostMapping("/insert")
    public ResponseEntity<Room> insert(@RequestBody Room entity) {
        Room obj = service.insert(entity);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(obj.getId()).toUri();
        return ResponseEntity.created(uri).body(obj);
    }

    @PostMapping("/insert-bed/{id}")
    public ResponseEntity<Room> insertBed(@PathVariable Long id) {
        var obj = service.insertBed(id);
        return ResponseEntity.ok().body(obj);
    }

    @PostMapping("/remove-bed/{id}")
    public ResponseEntity<Room> removeBed(@PathVariable Long id) {
        var obj = service.removeBed(id);
        return ResponseEntity.ok().body(obj);
    } 

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id){
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Room> update(@PathVariable Long id, @RequestBody Room entity) {
        Room obj = service.update(id, entity);
        return ResponseEntity.ok().body(obj);
    }

}