package com.marcos.clientesscout.service;

import com.marcos.clientesscout.dto.PageSpeedResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio de análisis técnico de sitios web.
 *
 * Funcionalidades:
 * - getScore(): rendimiento via Google PageSpeed Insights API.
 * - detectTechStack(): detección de tecnologías por análisis HTML (heurístico).
 * - getAiIssues(): generación de issues técnicas con Gemini AI.
 * - scrapeEmail(): extracción de email de contacto desde el HTML del sitio.
 *
 * NOTA LEGAL: el scraping de emails requiere cumplir con la política de uso del sitio web
 * objetivo (robots.txt, ToS). Este servicio es para uso interno de prospección comercial.
 */
@Service
public class AnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerService.class);

    // Cliente dedicado para PageSpeed Insights (base URL fija)
    private final WebClient pageSpeedClient;

    // Cliente genérico reutilizable para detectTechStack y Gemini
    // BUG CORREGIDO: antes se creaba un nuevo WebClient.create() en cada llamada a detectTechStack()
    // lo que generaba un nuevo pool de conexiones por invocación, desperdiciando recursos.
    private final WebClient generalClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${gemini.api.key}")
    private String aiApiKey;

    public AnalyzerService(WebClient.Builder webClientBuilder) {
        this.pageSpeedClient = webClientBuilder
                .baseUrl("https://www.googleapis.com/pagespeedonline/v5/runPagespeed")
                .build();
        // Cliente sin baseUrl fija para peticiones a dominios variables (tech stack, scraping)
        this.generalClient = WebClient.create();
    }

    /**
     * Obtiene la puntuación de rendimiento de una URL mediante Google PageSpeed Insights.
     *
     * La API de Google devuelve el score entre 0.0 y 1.0; se convierte a escala 0-100.
     *
     * @param url URL pública a analizar
     * @return Puntuación 0-100, o 0.0 si la URL no es accesible o hay error de API
     */
    public Double getScore(String url) {
        try {
            PageSpeedResponse response = this.pageSpeedClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("url", url)
                            .queryParam("category", "PERFORMANCE")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(PageSpeedResponse.class)
                    .block();

            if (response != null
                    && response.getLighthouseResult() != null
                    && response.getLighthouseResult().getCategories() != null
                    && response.getLighthouseResult().getCategories().getPerformance() != null) {

                Double score = response.getLighthouseResult().getCategories().getPerformance().getScore();
                return (score != null) ? score * 100 : 0.0;
            }
        } catch (Exception e) {
            log.error("PageSpeed analysis failed for URL {}: {}", url, e.getMessage());
        }
        return 0.0;
    }

    /**
     * Detecta el stack tecnológico de un sitio web por análisis heurístico del HTML.
     *
     * Método: descarga el HTML de la URL y busca patrones característicos de cada tecnología.
     * Es una detección de «best effort»: puede producir falsos positivos si el HTML contiene
     * las cadenas de texto fuera de contexto (p.ej. un artículo que mencione "wordpress").
     *
     * Timeout de 8 segundos para no bloquear el proceso de scouting con sitios lentos.
     *
     * @param url URL a analizar
     * @return Lista de tecnologías detectadas separadas por coma, "Custom / Other" si ninguna, o "Not Reachable"
     */
    public String detectTechStack(String url) {
        try {
            String html = generalClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(8))
                    .block();

            if (html == null) return "Unknown";

            String htmlLower = html.toLowerCase();
            StringBuilder techs = new StringBuilder();

            if (htmlLower.contains("wp-content")) techs.append("WordPress, ");
            if (htmlLower.contains("react")) techs.append("React, ");
            if (htmlLower.contains("_next")) techs.append("Next.js, ");
            if (htmlLower.contains("shopify")) techs.append("Shopify, ");
            if (htmlLower.contains("bootstrap")) techs.append("Bootstrap, ");
            if (htmlLower.contains("google-analytics") || htmlLower.contains("gtag")) techs.append("Google Analytics, ");
            if (htmlLower.contains("jquery")) techs.append("jQuery, ");
            if (htmlLower.contains("wix.com")) techs.append("Wix, ");
            if (htmlLower.contains("elementor")) techs.append("Elementor, ");

            String result = techs.toString();
            return result.isEmpty() ? "Custom / Other" : result.substring(0, result.length() - 2);
        } catch (Exception e) {
            return "Not Reachable";
        }
    }

    /**
     * Llama a la API de Gemini para generar 3 problemas técnicos específicos del negocio.
     *
     * El prompt incluye el nombre del negocio, su score de rendimiento y las tecnologías
     * detectadas para dar contexto a la IA y obtener sugerencias relevantes.
     *
     * @param businessName Nombre del negocio
     * @param score        Puntuación de rendimiento 0-100
     * @param techs        Tecnologías detectadas
     * @return Lista de líneas con los problemas detectados por la IA
     */
    public List<String> getAiIssues(String businessName, Double score, String techs) {
        String prompt = String.format(
                "As a web development expert, list 3 specific technical pain points for the business '%s'. "
                        + "Its website has a performance score of %.2f/100 and uses these technologies: %s. "
                        + "The response must be a simple list of 3 short sentences in English.",
                businessName, score, techs);

        try {
            // La API key de Gemini se incluye como query param (método estándar de Google AI Studio).
            // No registrar esta URL completa en logs para no exponer la clave.
            String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + aiApiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            String response = generalClient.post()
                    .uri(geminiUrl)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String aiText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            return List.of(aiText.split("\n"));

        } catch (Exception e) {
            log.error("Gemini AI call failed for business '{}': {}", businessName, e.getMessage());
            return List.of("Could not generate AI issues at this time.");
        }
    }

    /**
     * Extrae el email de contacto de un sitio web.
     *
     * Estrategia en dos pasos:
     * 1. Busca en el texto visible del HTML con regex de email.
     * 2. Si no encuentra, busca enlaces <a href="mailto:..."> en el DOM.
     *
     * Retorna valores especiales (no email) en caso de fallo:
     * - "N/A"           : URL inválida o de Google
     * - "Not found"     : sitio accesible pero sin email visible
     * - "Not reachable" : sitio no accesible (timeout, error HTTP)
     *
     * Nota: estos valores especiales se filtran en ClientService antes de persistir
     * para evitar guardar strings inválidos en el campo email (validado con @Email).
     *
     * @param url URL del sitio web a analizar
     * @return Email encontrado, o uno de los valores especiales descritos
     */
    public String scrapeEmail(String url) {
        if (url == null || url.equals("NO WEBSITE") || url.contains("google.com")) {
            return "N/A";
        }

        try {
            Document doc = Jsoup.connect(url)
                    // User-Agent estándar de navegador para evitar bloqueos por bots conocidos.
                    // Esto no garantiza acceso; algunos sitios requieren JS o tienen CAPTCHAs.
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // Paso 1: regex en el texto plano del documento
            Pattern emailPattern = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
            Matcher matcher = emailPattern.matcher(doc.text());
            if (matcher.find()) {
                return matcher.group();
            }

            // Paso 2: buscar en atributos href de enlaces mailto
            String mailto = doc.select("a[href^=mailto:]").attr("href").replace("mailto:", "");
            return mailto.isEmpty() ? "Not found" : mailto;

        } catch (Exception e) {
            log.warn("Email scraping failed for {}: {}", url, e.getMessage());
            return "Not reachable";
        }
    }
}
