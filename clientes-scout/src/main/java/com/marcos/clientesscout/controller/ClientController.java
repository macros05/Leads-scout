package com.marcos.clientesscout.controller;

import com.marcos.clientesscout.model.Client;
import com.marcos.clientesscout.service.AnalyzerService;
import com.marcos.clientesscout.service.ClientService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para la gestión de leads (clientes potenciales).
 *
 * Todos los endpoints requieren autenticación JWT (configurado en SecurityConfig).
 * El endpoint /scout lanza el proceso de búsqueda de forma asíncrona y retorna
 * inmediatamente para no bloquear al cliente HTTP.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

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

    /**
     * Actualiza los campos editables de un lead.
     * NOTA: este método duplica parcialmente la lógica de ClientService.update().
     * Se mantiene la versión del controlador que pasa la validación @Valid, pero
     * se delega el guardado al servicio para centralizar la lógica.
     */
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

    /**
     * Analiza el rendimiento de una URL usando Google PageSpeed Insights.
     *
     * SEGURIDAD (SSRF): este endpoint valida que la URL sea HTTP/HTTPS público
     * antes de hacer la petición saliente. Sin esta validación, un atacante
     * autenticado podría usar el servidor como proxy para escanear la red interna
     * o acceder a endpoints de metadatos cloud (169.254.169.254, etc.).
     *
     * @param url URL pública a analizar (debe comenzar con http:// o https://)
     * @return Puntuación de rendimiento 0-100
     */
    @GetMapping("/test-speed")
    public ResponseEntity<?> testSpeed(@RequestParam String url) {
        if (!isPublicHttpUrl(url)) {
            return ResponseEntity.badRequest()
                    .body("Invalid URL. Only public http:// or https:// URLs are allowed.");
        }
        Double score = analyzerService.getScore(url);
        return ResponseEntity.ok("Performance score for " + url + ": " + score);
    }

    /**
     * Inicia el proceso de scouting de negocios por sector de forma asíncrona.
     * Retorna inmediatamente mientras el proceso continúa en background.
     *
     * @param sector Sector a buscar (p.ej. "Clínica Dental Madrid"). Max 200 chars.
     */
    @PostMapping(value = "/scout", produces = "text/plain")
    @ResponseBody
    public ResponseEntity<String> scout(@RequestParam String sector) {
        if (sector == null || sector.isBlank()) {
            return ResponseEntity.badRequest().body("Sector parameter cannot be empty.");
        }
        if (sector.length() > 200) {
            return ResponseEntity.badRequest().body("Sector parameter exceeds maximum length of 200 characters.");
        }

        // Se usa @Async en lugar de new Thread() para aprovechar el pool de threads
        // configurado en Spring y poder monitorizar/controlar la ejecución.
        service.startScoutingAsync(sector);

        log.info("Scouting process initiated for sector: {}", sector);
        return ResponseEntity.accepted().body("Scouting process started for sector: " + sector);
    }

    /**
     * Valida que una URL sea HTTP/HTTPS y apunte a un host no privado.
     * Previene ataques SSRF (Server-Side Request Forgery).
     */
    private boolean isPublicHttpUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            String host = uri.getHost();
            if (host == null) return false;

            // Bloquear rangos privados y metadatos cloud
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("192.168.")
                    || host.startsWith("10.") || host.startsWith("172.")
                    || host.equals("169.254.169.254") || host.endsWith(".internal")) {
                return false;
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
