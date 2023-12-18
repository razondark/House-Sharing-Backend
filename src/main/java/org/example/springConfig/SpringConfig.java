package org.example.springConfig;

import org.example.hibernateConnector.HibernateSessionController;
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
