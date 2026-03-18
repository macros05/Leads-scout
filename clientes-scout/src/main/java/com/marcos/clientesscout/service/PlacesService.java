package com.marcos.clientesscout.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Servicio de integración con Google Places API (Text Search y Place Details).
 *
 * Uso principal: búsqueda de negocios locales por sector y obtención de su URL de sitio web.
 *
 * SEGURIDAD: las URLs construidas para la API incluyen la API key como query param,
 * que es el método estándar de Google. Asegurarse de que los logs no registren
 * la URL completa con la clave; este servicio usa log.debug solo para estado, no para URLs.
 */
@Service
public class PlacesService {

    private static final Logger log = LoggerFactory.getLogger(PlacesService.class);

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${google.api.key}")
    private String apiKey;

    public PlacesService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://maps.googleapis.com/maps/api/place").build();
    }

    /**
     * Realiza una búsqueda de texto en Google Places y devuelve el JSON de resultados.
     *
     * SEGURIDAD: el parámetro query se codifica con URLEncoder para prevenir
     * inyección de parámetros en la URL. Antes se hacía solo un .replace(" ", "%20")
     * que no cubría otros caracteres especiales como &, =, #, etc.
     *
     * @param query Término de búsqueda (p.ej. "Clínica Dental Madrid")
     * @return JsonNode con la respuesta de Google Places, o nodo vacío si hay error
     */
    public JsonNode searchBusinesses(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + encodedQuery
                + "&key=" + apiKey;

        try {
            // Leemos como String y parseamos manualmente para mayor control de errores
            // y para evitar problemas de deserialización con campos desconocidos de la API
            String responseBody = this.webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return mapper.readTree(responseBody);

        } catch (Exception e) {
            log.error("Error calling Google Places Text Search API: {}", e.getMessage());
            return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        }
    }

    /**
     * Obtiene la URL del sitio web de un negocio a partir de su Place ID.
     *
     * Solo solicita el campo "website" para minimizar el uso de quota de la API.
     *
     * @param placeId ID de lugar de Google Places
     * @return URL del sitio web, o null si no tiene o hay error
     */
    public String getWebsiteUrl(String placeId) {
        String url = "https://maps.googleapis.com/maps/api/place/details/json?place_id="
                + URLEncoder.encode(placeId, StandardCharsets.UTF_8)
                + "&fields=website&key=" + apiKey;

        try {
            String responseBody = this.webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = mapper.readTree(responseBody);
            return node.path("result").path("website").asText(null);

        } catch (Exception e) {
            log.error("Error calling Google Places Details API for placeId {}: {}", placeId, e.getMessage());
            return null;
        }
    }
}
