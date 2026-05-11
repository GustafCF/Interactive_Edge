package com.br.elohostel.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        // Este endpoint ainda pode ser útil, mas não é mais necessário para o estabelecimento
        return ResponseEntity.ok().body(null);
    }
    
    // NOVO ENDPOINT: Recebe o email do corpo da requisição
    @PostMapping("/establishment-info")
    public ResponseEntity<?> getEstablishmentInfo(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            System.out.println("=== getEstablishmentInfo via POST ===");
            System.out.println("Email recebido: " + email);
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email não fornecido"));
            }
            
            User user = service.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Usuário não encontrado: " + email));
            }
            
            Map<String, String> info = new HashMap<>();
            info.put("establishmentName", user.getEstablishmentName() != null ? user.getEstablishmentName() : "Elô AP");
            info.put("establishmentAddress", user.getEstablishmentAddress() != null ? user.getEstablishmentAddress() : "QNM 16 Conjunto B");
            info.put("establishmentPhone", user.getEstablishmentPhone() != null ? user.getEstablishmentPhone() : "(61) 99999-9999");
            info.put("establishmentResponsible", user.getEstablishmentResponsible() != null ? user.getEstablishmentResponsible() : user.getName());
            info.put("establishmentLogo", user.getEstablishmentLogo() != null ? user.getEstablishmentLogo() : "");
            info.put("establishmentWelcomeMessage", user.getEstablishmentWelcomeMessage() != null ? user.getEstablishmentWelcomeMessage() : "");
            
            return ResponseEntity.ok().body(info);
        } catch (Exception e) {
            System.err.println("ERRO em getEstablishmentInfo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/update-establishment-info")
    public ResponseEntity<?> updateEstablishmentInfo(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            System.out.println("=== updateEstablishmentInfo ===");
            System.out.println("Email recebido: " + email);
            System.out.println("Dados recebidos: " + request);
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email não fornecido"));
            }
            
            User user = service.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Usuário não encontrado: " + email));
            }
            
            if (request.containsKey("establishmentName")) {
                user.setEstablishmentName(request.get("establishmentName"));
            }
            if (request.containsKey("establishmentAddress")) {
                user.setEstablishmentAddress(request.get("establishmentAddress"));
            }
            if (request.containsKey("establishmentPhone")) {
                user.setEstablishmentPhone(request.get("establishmentPhone"));
            }
            if (request.containsKey("establishmentResponsible")) {
                user.setEstablishmentResponsible(request.get("establishmentResponsible"));
            }
            if (request.containsKey("establishmentLogo")) {
                user.setEstablishmentLogo(request.get("establishmentLogo"));
            }
            if (request.containsKey("establishmentWelcomeMessage")) {
                user.setEstablishmentWelcomeMessage(request.get("establishmentWelcomeMessage"));
            }
            
            User updatedUser = service.update(user.getId(), user);
            System.out.println("Usuário atualizado com sucesso!");
            
            return ResponseEntity.ok().body(updatedUser);
        } catch (Exception e) {
            System.err.println("ERRO em updateEstablishmentInfo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/is")
    public ResponseEntity<User> insert(@RequestBody CreateUserDto dto) {
        var user = service.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(uri).body(user);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody CreateUserDto dto) {
        var user = service.register(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(uri).body(user);
    }

    @DeleteMapping("/dl/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/up/role")
    public ResponseEntity<User> updateRole(@RequestBody String name, @RequestBody String nameRole) {
        User us = service.updateRole(name, nameRole);
        return ResponseEntity.ok().body(us);
    }

    @PutMapping("/up/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User obj) {
        var user = service.update(id, obj);
        return ResponseEntity.ok().body(user);
    }
}