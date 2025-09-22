package com.ialegal.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.ialegal.backend.repository")
public class JpaConfig {
    // Esta configuración habilita el auditing automático para @CreatedDate y @LastModifiedDate
    // También especifica el paquete base para los repositorios JPA
}