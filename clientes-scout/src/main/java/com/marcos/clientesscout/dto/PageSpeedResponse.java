package com.marcos.clientesscout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageSpeedResponse {
    private LighthouseResult lighthouseResult;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LighthouseResult {
        private Categories categories;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Categories {
        private Performance performance;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Performance {
        private Double score; // Google devuelve de 0.0 a 1.0
    }
}