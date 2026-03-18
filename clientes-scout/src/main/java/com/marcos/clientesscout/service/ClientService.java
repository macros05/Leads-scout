package com.marcos.clientesscout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marcos.clientesscout.model.ApplicationStatus;
import com.marcos.clientesscout.model.Client;
import com.marcos.clientesscout.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal de gestión de leads.
 *
 * Responsabilidades:
 * - CRUD de entidades Client.
 * - Orquestación del proceso de scouting automático (Google Places + análisis web).
 * - Consultas de negocio (estadísticas, filtros por presupuesto, contactos semanales).
 *
 * MEJORA PENDIENTE: getWeeklyContacts() y getStatusStats() cargan todos los registros
 * en memoria y filtran en Java. Con grandes volúmenes de datos deben migrarse a consultas
 * JPQL con cláusulas WHERE/GROUP BY en ClientRepository.
 */
@Service
public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    @Autowired
    private ClientRepository repository;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private PlacesService placesService;

    public List<Client> findAll() {
        return repository.findAll();
    }

    public Optional<Client> findById(Long id) {
        return repository.findById(id);
    }

    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    public void deleteById(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Cannot delete: Client not found with id: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Guarda un nuevo lead tras validar sus datos de negocio.
     * La validación de formato de campos (@Email, @Pattern) la gestiona la capa de controlador
     * mediante @Valid; esta validación cubre reglas adicionales de negocio.
     */
    public Client save(Client client) {
        validate(client);
        return repository.save(client);
    }

    /**
     * Actualiza los campos editables de un lead existente.
     * No actualiza websiteUrl, performanceScore ni detectedIssues para proteger
     * los datos generados por el proceso de análisis automático.
     */
    public Client update(Long id, Client details) {
        Client client = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        client.setName(details.getName());
        client.setTechStack(details.getTechStack());
        client.setBudget(details.getBudget());
        client.setContactDate(details.getContactDate());
        client.setStatus(details.getStatus());

        return repository.save(client);
    }

    public List<Client> findHighBudget(int threshold) {
        return repository.findByBudgetGreaterThanEqual(threshold);
    }

    /**
     * Devuelve los leads con contactDate en los próximos 7 días (incluyendo hoy).
     *
     * MEJORA PENDIENTE: migrar a query JPQL para evitar cargar todos los registros en memoria:
     *   @Query("SELECT c FROM Client c WHERE c.contactDate BETWEEN :today AND :nextWeek")
     */
    public List<Client> getWeeklyContacts() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        return repository.findAll().stream()
                .filter(c -> c.getContactDate() != null
                        && c.getContactDate().isAfter(today.minusDays(1))
                        && c.getContactDate().isBefore(nextWeek))
                .toList();
    }

    /**
     * Devuelve un mapa con el conteo de leads por estado.
     *
     * MEJORA PENDIENTE: migrar a query JPQL con GROUP BY para rendimiento en producción:
     *   @Query("SELECT c.status, COUNT(c) FROM Client c GROUP BY c.status")
     */
    public Map<ApplicationStatus, Long> getStatusStats() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(Client::getStatus, Collectors.counting()));
    }

    /**
     * Punto de entrada asíncrono para el proceso de scouting.
     * Llamado desde el controlador; delega en startScouting() que se ejecuta
     * en el pool de threads de Spring (@Async).
     */
    @Async
    public void startScoutingAsync(String sector) {
        startScouting(sector);
    }

    /**
     * Orquesta el proceso de scouting para un sector dado:
     * 1. Busca negocios con Google Places Text Search.
     * 2. Para cada negocio con web: analiza rendimiento, tecnologías, genera issues con IA y extrae email.
     * 3. Para negocios sin web: los registra como leads de "riesgo crítico" (oportunidad de venta).
     * 4. Omite negocios cuya URL ya existe en la base de datos para evitar duplicados.
     *
     * SEGURIDAD: el parámetro sector ya está validado en el controlador (no vacío, max 200 chars).
     * Las URLs obtenidas de Google Places se pasan a AnalyzerService que hace peticiones salientes;
     * se confía en que Google Places no devuelva URLs internas, pero considerar añadir validación extra.
     */
    void startScouting(String sector) {
        log.info("Starting scouting for sector: {}", sector);

        JsonNode root = placesService.searchBusinesses(sector);

        if (root == null || root.isEmpty()) {
            log.error("PlacesService response is null or empty for sector: {}", sector);
            return;
        }

        String status = root.path("status").asText("UNKNOWN");

        log.debug("Google Places API status: {}", status);

        if ("REQUEST_DENIED".equals(status)) {
            String errorMessage = root.path("error_message").asText("No additional error message.");
            log.error("Google Places denied the request. Reason: {}", errorMessage);
            return;
        }

        JsonNode results = root.path("results");

        if (!results.isArray()) {
            log.error("Google Places 'results' field is not an array. Raw response: {}", root);
            return;
        }

        log.info("Found {} potential businesses for sector: {}", results.size(), sector);

        for (JsonNode node : results) {
            try {
                String name = node.path("name").asText();
                String placeId = node.path("place_id").asText();
                // formatted_phone_number no siempre está presente en la respuesta de Text Search
                String phone = node.path("formatted_phone_number").asText(null);

                log.debug("Processing business: {}", name);
                String website = placesService.getWebsiteUrl(placeId);

                Client prospect = new Client();
                prospect.setName(name);
                prospect.setPhone(phone);
                prospect.setStatus(ApplicationStatus.INTERVIEW);
                prospect.setContactDate(LocalDate.now());
                prospect.setBudget(0);

                if (website != null && !website.isBlank() && !website.equalsIgnoreCase("null")) {
                    // Negocio CON sitio web: analizar rendimiento y tecnologías
                    if (!repository.existsByWebsiteUrl(website)) {
                        log.debug("Analyzing website for: {} ({})", name, website);

                        Double score = analyzerService.getScore(website);
                        String techs = analyzerService.detectTechStack(website);
                        List<String> issues = analyzerService.getAiIssues(name, score, techs);
                        String email = analyzerService.scrapeEmail(website);

                        prospect.setWebsiteUrl(website);
                        prospect.setPerformanceScore(score);
                        prospect.setTechStack(techs);
                        prospect.setDetectedIssues(issues);
                        // BUG CORREGIDO: el email se extraía pero nunca se asignaba al prospecto
                        if (email != null && !email.equals("N/A") && !email.equals("Not found")
                                && !email.equals("Not reachable")) {
                            prospect.setEmail(email);
                        }

                        repository.save(prospect);
                        log.info("Lead saved: {} | score={} | email={}", name, score, email);
                    } else {
                        log.info("Skipping {} — website already exists in database: {}", name, website);
                    }
                } else {
                    // Negocio SIN sitio web: registrar como oportunidad de máxima prioridad
                    log.info("No website found for '{}' — registering as critical-priority lead", name);

                    prospect.setWebsiteUrl("NO WEBSITE");
                    prospect.setPerformanceScore(0.0);
                    prospect.setTechStack("None");
                    prospect.setDetectedIssues(List.of(
                            "Critical: No digital presence detected.",
                            "Business is invisible to 90% of local digital searches.",
                            "Urgent need for a professional landing page to capture leads."
                    ));

                    repository.save(prospect);
                    log.info("Critical-priority lead saved: {}", name);
                }
            } catch (Exception e) {
                // Se captura por negocio para que un fallo individual no detenga todo el lote
                log.error("Error processing business '{}': {}", node.path("name").asText(), e.getMessage(), e);
            }
        }

        if (results.isEmpty() && "OK".equals(status)) {
            log.warn("Google Places returned no results for sector: {}", sector);
        }

        log.info("Scouting finished for sector: {}", sector);
    }

    /**
     * Validaciones de negocio para un lead.
     * Se ejecuta antes de persistir tanto en save() como indirectamente en update().
     */
    private void validate(Client client) {
        if (client.getName() == null || client.getName().isEmpty()) {
            throw new IllegalArgumentException("Client name is required");
        }
        if (client.getBudget() < 0) {
            throw new IllegalArgumentException("Budget cannot be negative");
        }
    }
}
