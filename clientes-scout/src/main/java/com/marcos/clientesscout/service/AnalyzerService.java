package com.marcos.clientesscout.service;

import com.marcos.clientesscout.dto.PageSpeedResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnalyzerService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${gemini.api.key}")
    private String aiApiKey;

    public AnalyzerService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://www.googleapis.com/pagespeedonline/v5/runPagespeed").build();
    }

    public Double getScore(String url) {
        try {
            PageSpeedResponse response = this.webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("url", url)
                            .queryParam("category", "PERFORMANCE")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(PageSpeedResponse.class)
                    .block();

            if (response != null &&
                    response.getLighthouseResult() != null &&
                    response.getLighthouseResult().getCategories() != null &&
                    response.getLighthouseResult().getCategories().getPerformance() != null) {

                Double score = response.getLighthouseResult().getCategories().getPerformance().getScore();
                return (score != null) ? score * 100 : 0.0;
            }
        } catch (Exception e) {
            System.err.println("Error analyzing URL: " + e.getMessage());
        }
        return 0.0;
    }

    public String detectTechStack(String url) {
        try {
            WebClient generalClient = WebClient.create();
            String html = generalClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(8))
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



    public List<String> getAiIssues(String businessName, Double score, String techs) {
        String prompt = String.format(
                "As a web development expert, list 3 specific technical pain points for the business '%s'. " +
                        "Its website has a performance score of %.2f/100 and uses these technologies: %s. " +
                        "The response must be a simple list of 3 short sentences in English.",
                businessName, score, techs);

        try {
            WebClient aiClient = WebClient.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + aiApiKey);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            String response = aiClient.post()
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String aiText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            return List.of(aiText.split("\n"));

        } catch (Exception e) {
            System.err.println("Error calling Gemini IA: " + e.getMessage());
            return List.of("Could not generate AI issues at this time.");
        }
    }

    public String scrapeEmail(String url) {
        if (url == null || url.equals("NO WEBSITE") || url.contains("google.com")) {
            return "N/A";
        }

        try {
            // Conectamos con un User-Agent para que la web no nos bloquee pensando que somos un bot malo
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // Regex para detectar emails
            Pattern p = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");
            Matcher m = p.matcher(doc.text());

            if (m.find()) {
                return m.group();
            }

            // Si no lo encuentra en el texto, buscamos en los enlaces "mailto:"
            String mailto = doc.select("a[href^=mailto:]").attr("href").replace("mailto:", "");
            return mailto.isEmpty() ? "Not found" : mailto;

        } catch (Exception e) {
            System.err.println("Scraper error for " + url + ": " + e.getMessage());
            return "Not reachable";
        }
    }
}