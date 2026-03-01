package com.marcos.clientesscout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marcos.clientesscout.model.ApplicationStatus;
import com.marcos.clientesscout.model.Client;
import com.marcos.clientesscout.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClientService {

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

    public Client save(Client client) {
        validate(client);
        return repository.save(client);
    }

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

    public List<Client> getWeeklyContacts() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        return repository.findAll().stream()
                .filter(c -> c.getContactDate() != null
                        && c.getContactDate().isAfter(today.minusDays(1))
                        && c.getContactDate().isBefore(nextWeek))
                .toList();
    }

    public Map<ApplicationStatus, Long> getStatusStats() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(Client::getStatus, Collectors.counting()));
    }

    public void startScouting(String sector) {
        System.out.println("DEBUG: Starting scouting for: " + sector);

        JsonNode root = placesService.searchBusinesses(sector);

        if (root == null || root.isEmpty()) {
            System.err.println("ERROR: PlacesService response is NULL or EMPTY.");
            return;
        }

        String status = root.path("status").asText("UNKNOWN");
        String errorMessage = root.path("error_message").asText("No additional error message.");

        System.out.println("DEBUG: Google API status: " + status);

        if (status.equals("REQUEST_DENIED")) {
            System.err.println("WARNING: Google denied the request.");
            System.err.println("REASON: " + errorMessage);
            return;
        }

        JsonNode results = root.path("results");

        if (!results.isArray()) {
            System.err.println("ERROR: 'results' is not an array.");
        } else {
            System.out.println("DEBUG: Found " + results.size() + " potential businesses.");
        }

        if (results.isArray() && !results.isEmpty()) {
            for (JsonNode node : results) {
                try {
                    String name = node.path("name").asText();
                    String placeId = node.path("place_id").asText();
                    String phone = node.path("formatted_phone_number").asText(null);

                    System.out.println("DEBUG: Processing: " + name);
                    String website = placesService.getWebsiteUrl(placeId);


                    Client prospect = new Client();
                    prospect.setName(name);
                    prospect.setPhone(phone);
                    prospect.setStatus(ApplicationStatus.INTERVIEW);
                    prospect.setContactDate(LocalDate.now());
                    prospect.setBudget(0);

                    // Lógica para negocios CON sitio web
                    if (website != null && !website.isEmpty() && !website.equalsIgnoreCase("null")) {
                        if (!repository.existsByWebsiteUrl(website)) {
                            System.out.println("DEBUG: Analyzing tech for: " + website);

                            Double score = analyzerService.getScore(website);
                            String techs = analyzerService.detectTechStack(website);
                            List<String> issues = analyzerService.getAiIssues(name, score, techs);

                            String email = analyzerService.scrapeEmail(website);


                            prospect.setWebsiteUrl(website);
                            prospect.setPerformanceScore(score);
                            prospect.setTechStack(techs);
                            prospect.setDetectedIssues(issues);

                            repository.save(prospect);
                            System.out.println("SUCCESS: Lead saved -> " + name);
                        } else {
                            System.out.println("INFO: " + name + " already exists, skipping...");
                        }
                    }
                    // Lógica para negocios SIN sitio web (CRITICAL RISK)
                    else {
                        System.out.println("CRITICAL: " + name + " has no website. Registering as top priority lead.");

                        prospect.setWebsiteUrl("NO WEBSITE");
                        prospect.setPerformanceScore(0.0);
                        prospect.setTechStack("None");

                        // Generamos fallos predefinidos para la venta de una nueva web
                        prospect.setDetectedIssues(List.of(
                                "Critical: No digital presence detected.",
                                "Business is invisible to 90% of local digital searches.",
                                "Urgent need for a professional landing page to capture leads."
                        ));

                        repository.save(prospect);
                        System.out.println("SUCCESS: Critical Risk lead saved -> " + name);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing lead (" + node.path("name").asText() + "): " + e.getMessage());
                }
            }
        } else if (results.isArray() && results.isEmpty() && status.equals("OK")) {
            System.out.println("WARNING: Google found no results for: " + sector);
        }

        System.out.println("DEBUG: Scouting finished.");
    }

    private void validate(Client client) {
        if (client.getName() == null || client.getName().isEmpty()) {
            throw new IllegalArgumentException("Client name is required");
        }
        if (client.getBudget() < 0) {
            throw new IllegalArgumentException("Budget cannot be negative");
        }
    }


}