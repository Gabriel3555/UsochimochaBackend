package com.app.usochicamochabackend.auth.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    private Boolean status = true;

    @Column(name = "username")
    private String username;

    private String email;
    private String role;
    private String password;

    @Column(name = "license_category", length = 50)
    private String licenseCategory;

    @Column(name = "license_expiry")
    private java.time.LocalDate licenseExpiry;

    @Column(name = "license_document_url", length = 1024)
    private String licenseDocumentUrl;
}
