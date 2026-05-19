package com.tso.userprogress.security;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;

  public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  /**
   * Password encoder bean using BCrypt.
   *
   * @return BCryptPasswordEncoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Configure CORS settings.
   *
   * @return CorsConfigurationSource with allowed origins, methods, and headers
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // Allow requests from localhost (development)
    configuration.setAllowedOrigins(
        Arrays.asList("http://localhost:3000", "http://localhost:5173", "http://localhost:80"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /**
   * Configure security filter chain for stateless JWT authentication.
   *
   * @param http HttpSecurity configuration
   * @return SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Enable CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // Disable CSRF for stateless JWT
        .csrf(csrf -> csrf.disable())
        // Use stateless session policy
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Configure endpoint access
        .authorizeHttpRequests(
            authz ->
                authz
                    // Public endpoints
                    .requestMatchers(HttpMethod.POST, "/user-progress/auth/signup")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/user-progress/auth/login")
                    .permitAll()
                    .requestMatchers("/user-progress/health")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                    .permitAll()
                    // Protected endpoints - require JWT authentication
                    .requestMatchers(HttpMethod.GET, "/user-progress/auth/me")
                    .authenticated()
                    // All other requests require authentication
                    .anyRequest()
                    .authenticated())
        // Add JWT filter
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
