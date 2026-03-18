package com.marcos.clientesscout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punto de entrada de la aplicación Leads Scout.
 *
 * @EnableAsync habilita el soporte de Spring para métodos @Async,
 * necesario para que ClientService.startScoutingAsync() se ejecute
 * en un hilo del pool de threads gestionado por Spring en lugar de
 * crear hilos crudos con new Thread() en el controlador.
 */
@SpringBootApplication
@EnableAsync
public class ClientesScoutApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientesScoutApplication.class, args);
    }
}
