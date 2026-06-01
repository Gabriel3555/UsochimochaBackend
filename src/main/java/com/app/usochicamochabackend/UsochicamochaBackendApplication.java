package com.app.usochicamochabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UsochicamochaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsochicamochaBackendApplication.class, args);
    }


}
