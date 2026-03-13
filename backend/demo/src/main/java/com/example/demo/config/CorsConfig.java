package com.example.demo.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = parseOrigins(allowedOrigins);

        var mapping = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        if (Arrays.stream(origins).anyMatch("*"::equals)) {
            mapping.allowedOriginPatterns("*");
        } else {
            mapping.allowedOrigins(origins);
        }
    }

    private String[] parseOrigins(String originConfig) {
        if (originConfig == null || originConfig.isBlank()) {
            return new String[] {"*"};
        }

        return Arrays.stream(originConfig.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }
}
