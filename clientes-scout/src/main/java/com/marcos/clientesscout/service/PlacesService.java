package com.marcos.clientesscout.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;

@Service
public class PlacesService {

    private final WebClient webClient;

    @Value("${google.api.key}")
    private String apiKey;

    public PlacesService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://maps.googleapis.com/maps/api/place").build();
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode searchBusinesses(String query) {
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + query.replace(" ", "%20")
                + "&key=" + apiKey;

        try {
            // Leemos como String para evitar errores de Jackson con WebClient
            String responseBody = this.webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return mapper.readTree(responseBody);

        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO EN BÚSQUEDA: " + e.getMessage());
            return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        }
    }

    public String getWebsiteUrl(String placeId) {
        // Usamos la misma técnica: URL manual y respuesta como String
        String url = "https://maps.googleapis.com/maps/api/place/details/json?place_id="
                + placeId
                + "&fields=website&key=" + apiKey;

        try {
            String responseBody = this.webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Convertimos manualmente para extraer la web
            JsonNode node = mapper.readTree(responseBody);
            return node.path("result").path("website").asText(null);

        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO EN DETALLES: " + e.getMessage());
            return null;
        }
    }
}