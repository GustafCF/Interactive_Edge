package com.br.elohostel.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.elohostel.model.Bed;
import com.br.elohostel.service.BedService;

@RestController
@RequestMapping("/bed")
public class BedController {

    private final BedService service;

    public BedController(BedService service) {
        this.service = service;
    }

    @GetMapping("/findAll")
    public ResponseEntity<List<Bed>> findAll() {
        List<Bed> list = service.findAll();
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/vague")
    public ResponseEntity<List<Bed>> finByStatusVague() {
        List<Bed> list = service.findByBedStatusVague();
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/occupied")
    public ResponseEntity<List<Bed>> finByStatusOccupied() {
        List<Bed> list = service.findByBedStatusOccupied();
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/roomvague/{id}")
    public ResponseEntity<List<Bed>> findAvailableBedsByRoomVague(@PathVariable Long id) {
        List<Bed> list = service.findAvailableBedsByRoomVague(id);
        return ResponseEntity.ok().body(list);
    }
}
