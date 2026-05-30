package com.app.usochicamochabackend.config;

import com.app.usochicamochabackend.actions.infrastructure.repository.ActionRepository;
import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.update.infrastructure.entity.BrandEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.repository.OilChangeRepository;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.order.infrastructure.repository.OrderRepository;
import com.app.usochicamochabackend.performance.infrastructure.entity.LaborEntity;
import com.app.usochicamochabackend.performance.infrastructure.entity.ResultEntity;
import com.app.usochicamochabackend.performance.infrastructure.entity.SparePartEntity;
import com.app.usochicamochabackend.performance.infrastructure.repository.ResultRepository;
import com.app.usochicamochabackend.review.infrastructure.entity.ImageEntity;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
import com.app.usochicamochabackend.review.infrastructure.repository.ImageRepository;
import com.app.usochicamochabackend.review.infrastructure.repository.InspectionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@Profile("prod")
public class DataSeeder {

    @Bean
    CommandLineRunner initData(
            UserRepositoryJpa userRepository,
            MachineRepository machineRepository,
            InspectionRepository inspectionRepository,
            ImageRepository imageRepository,
            OrderRepository orderRepository,
            ResultRepository resultRepository,
            ActionRepository actionRepository,
            OilChangeRepository oilChangeRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Limpiar repos - COMENTADO PARA PRESERVAR DATOS DEL USUARIO
            /*
             * orderRepository.deleteAllInBatch();
             * resultRepository.deleteAllInBatch();
             * imageRepository.deleteAllInBatch();
             * inspectionRepository.deleteAllInBatch();
             * oilChangeRepository.deleteAllInBatch();
             * actionRepository.deleteAllInBatch();
             * machineRepository.deleteAllInBatch();
             * userRepository.deleteAllInBatch();
             */

            // === Usuarios ===
            // Solo creamos el admin si no existe para no borrar datos previos
            if (userRepository.findByUsername("admin").isEmpty()) {
                UserEntity admin = UserEntity.builder()
                        .fullName("Admin User")
                        .username("admin")
                        .password(passwordEncoder.encode("password")) // "password"
                        .email("admin@example.com")
                        .role("ADMIN")
                        .status(true)
                        .build();

                userRepository.save(admin);
            }
        };
    }
}
