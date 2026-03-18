package com.marcos.clientesscout.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Entidad que representa un lead (cliente potencial) en la base de datos.
 *
 * Los campos de análisis (performanceScore, techStack, detectedIssues, websiteUrl)
 * son poblados automáticamente por el proceso de scouting (ClientService.startScouting)
 * y no deben modificarse manualmente excepto a través de ese flujo.
 *
 * El campo email se valida con @Email cuando se introduce manualmente.
 * Cuando proviene del scraper (AnalyzerService.scrapeEmail), el servicio filtra
 * los valores especiales ("N/A", "Not found", "Not reachable") antes de asignarlos.
 */
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String name;

    @Email(message = "Email format is invalid")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "Phone number format is invalid")
    private String phone;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Column(length = 500)
    private String websiteUrl;

    /** Puntuación de rendimiento PageSpeed, en escala 0-100. Null si no analizado. */
    private Double performanceScore;

    /** Tecnologías detectadas en el sitio, separadas por coma. */
    private String techStack;

    /**
     * Lista de problemas técnicos generados por Gemini AI.
     * Almacenada en tabla secundaria (client_detected_issues) por @ElementCollection.
     */
    @ElementCollection
    private List<String> detectedIssues;

    private LocalDate contactDate;

    private int budget;

    // --- Getters y Setters ---

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public Double getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(Double performanceScore) { this.performanceScore = performanceScore; }

    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }

    public List<String> getDetectedIssues() { return detectedIssues; }
    public void setDetectedIssues(List<String> detectedIssues) { this.detectedIssues = detectedIssues; }

    public LocalDate getContactDate() { return contactDate; }
    public void setContactDate(LocalDate contactDate) { this.contactDate = contactDate; }

    public int getBudget() { return budget; }
    public void setBudget(int budget) { this.budget = budget; }
}
