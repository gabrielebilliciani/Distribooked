package it.unipi.distribooked.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "it.unipi.distribooked.mapper",
        "it.unipi.distribooked.service",
        "it.unipi.distribooked.repository"
})
public class AppConfig {
}

