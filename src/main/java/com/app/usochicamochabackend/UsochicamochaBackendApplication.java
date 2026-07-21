package com.app.usochicamochabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
public class UsochicamochaBackendApplication {

    /**
     * LocalDateTime.now() usado en toda la app depende de la zona horaria por defecto de la JVM
     * (ZoneId.systemDefault()), no de spring.jackson.time-zone ni de hibernate.jdbc.time_zone.
     * Si el host (VPS/contenedor) no tiene su TZ del sistema en America/Bogota, cada timestamp
     * generado por el backend queda desfasado. Se fija aquí, antes de cualquier otro código.
     */
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
    }

    public static void main(String[] args) {
        SpringApplication.run(UsochicamochaBackendApplication.class, args);
    }


}
