package org.example.config;

import org.example.hibernateController.HibernateSessionController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings("unused")
public class SpringConfig {
    @Bean
    public HibernateSessionController hibernateSessionController() {
        return new HibernateSessionController();
    }
}
