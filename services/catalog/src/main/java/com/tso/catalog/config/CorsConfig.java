package com.tso.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Allows the browser-based web-client to call the (unauthenticated) catalog endpoints. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  // Browser origins allowed to call this service (comma-separated). Defaults to the
  // local dev/compose web client; in Kubernetes the Helm chart injects the deployed
  // origin via APP_CORS_ALLOWED_ORIGINS, so no environment-specific value is hardcoded.
  @Value("${app.cors.allowed-origins:http://localhost:8080,http://localhost:5173}")
  private String[] allowedOrigins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**").allowedOrigins(allowedOrigins).allowedMethods("GET", "OPTIONS");
  }
}
