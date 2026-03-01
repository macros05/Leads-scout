package com.marcos.clientesscout.controller;

import com.marcos.clientesscout.model.Client;
import com.marcos.clientesscout.service.AnalyzerService;
import com.marcos.clientesscout.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService service;

    @Autowired
    private AnalyzerService analyzerService;

    @GetMapping
    public List<Client> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Client> create(@Valid @RequestBody Client client) {
        return ResponseEntity.status(201).body(service.save(client));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Client> update(@PathVariable Long id,
                                         @Valid @RequestBody Client client) {
        return service.findById(id).map(existing -> {
            existing.setName(client.getName());
            existing.setEmail(client.getEmail());
            existing.setPhone(client.getPhone());
            existing.setNotes(client.getNotes());
            existing.setStatus(client.getStatus());
            return ResponseEntity.ok(service.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!service.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/budget/{amount}")
    public List<Client> getByBudget(@PathVariable int amount) {
        return service.findHighBudget(amount);
    }

    @GetMapping("/test-speed")
    public String testSpeed(@RequestParam String url) {
        Double score = analyzerService.getScore(url);
        return "Performance score for " + url + ": " + score;
    }

    @PostMapping(value = "/scout", produces = "text/plain")
    @ResponseBody
    public String scout(@RequestParam String sector) {
        new Thread(() -> {
            try {
                service.startScouting(sector);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return "Scouting process started for sector: " + sector;
    }
}