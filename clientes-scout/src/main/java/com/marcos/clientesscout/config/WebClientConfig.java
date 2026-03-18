package com.marcos.clientesscout.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración del cliente HTTP reactivo (WebClient).
 *
 * Proporciona un WebClient.Builder con un límite de memoria aumentado (10 MB)
 * para manejar respuestas de APIs externas que pueden superar el límite por defecto
 * de 256 KB (Google PageSpeed, Gemini AI, Places).
 *
 * NOTA: La configuración CORS se gestiona exclusivamente en SecurityConfig
 * via CorsConfigurationSource. Se elimina addCorsMappings() de aquí porque
 * cuando Spring Security está activo, la configuración CORS de WebMvcConfigurer
 * puede ser ignorada o entrar en conflicto. Tener CORS en dos lugares causaba
 * ambigüedad y el origen estaba duplicado innecesariamente.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)); // 10 MB
    }
}
