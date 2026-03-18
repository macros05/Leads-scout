package com.marcos.clientesscout.config;

/**
 * Clase reservada para inicialización de datos en entorno de desarrollo.
 *
 * Actualmente vacía. Si se necesitan datos de prueba al arrancar la aplicación,
 * implementar CommandLineRunner aquí con @Profile("dev") para que solo se ejecute
 * en desarrollo y no en producción.
 *
 * Ejemplo de uso:
 *   @Bean
 *   @Profile("dev")
 *   public CommandLineRunner seedData(ClientRepository repo) {
 *       return args -> { ... };
 *   }
 */
@org.springframework.context.annotation.Configuration
public class DataLoader {
}
